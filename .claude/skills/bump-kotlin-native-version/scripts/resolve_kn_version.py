#!/usr/bin/env python3
"""Resolve and verify a Kotlin/Native version for the `kotlin.native.version.default` bump.

Two things have to be true before a version is safe to put into gradle.properties:

  1. It was published by the TeamCity configuration Kotlin_KotlinDev_KotlinNativePublishMaven
     from the `master` branch with status SUCCESS. Builds from feature branches also land in
     the dev repository, and pinning KGP to one of those means KGP is baked against a K/N that
     nothing else in the ecosystem is testing against.

  2. Every platform artifact is actually present in the dev Maven repository. KGP downloads
     `kotlin-native-prebuilt-<version>-<platform>.tar.gz` at build time on developer machines
     and in KGP integration tests, so a version missing the Windows zip breaks Windows agents
     only, days later.

Both endpoints are readable without credentials (TeamCity via /guestAuth), so this runs
anywhere without setup.

Usage:
    resolve_kn_version.py                      # resolve the newest master build
    resolve_kn_version.py --version 2.5.0-dev-4055   # verify a specific version
    resolve_kn_version.py --json               # machine-readable output only
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

TC_BUILDS_URL = (
    "https://buildserver.labs.intellij.net/guestAuth/app/rest/builds"
    "?locator=buildType:Kotlin_KotlinDev_KotlinNativePublishMaven,"
    "branch:default:true,status:SUCCESS,count:{count}"
    "&fields=build(number,status,branchName,finishDate,webUrl)"
)

DEV_REPO = "https://packages.jetbrains.team/maven/p/kt/dev"
ARTIFACT_PATH = "org/jetbrains/kotlin/kotlin-native-prebuilt"

# The platforms KGP can download a prebuilt distribution for. All of them must exist,
# otherwise the bump silently breaks whichever host isn't covered.
PLATFORM_FILES = [
    "{v}-macos-aarch64.tar.gz",
    "{v}-macos-x86_64.tar.gz",
    "{v}-linux-x86_64.tar.gz",
    "{v}-windows-x86_64.zip",
]

TIMEOUT = 30


def http_json(url: str) -> dict:
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return json.load(resp)


def artifact_exists(url: str) -> bool:
    req = urllib.request.Request(url, method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            return resp.status == 200
    except urllib.error.HTTPError:
        return False
    except urllib.error.URLError as exc:
        raise SystemExit(f"error: cannot reach {url}: {exc}") from exc


def read_property(props: Path, key: str) -> str | None:
    if not props.is_file():
        return None
    for line in props.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith(f"{key}="):
            return line.split("=", 1)[1].strip()
    return None


def dev_number(version: str) -> int | None:
    """Extract N from `X.Y.Z-dev-N`, used for ordering and downgrade detection."""
    m = re.fullmatch(r"\d+\.\d+\.\d+-dev-(\d+)", version)
    return int(m.group(1)) if m else None


def version_line(version: str) -> str | None:
    """`2.5.0-dev-4190` -> `2.5`. Used to check the bump stays on the current dev line."""
    m = re.match(r"(\d+\.\d+)\.", version)
    return m.group(1) if m else None


def latest_master_builds(count: int) -> list[dict]:
    data = http_json(TC_BUILDS_URL.format(count=count))
    return data.get("build", [])


def verify_artifacts(version: str) -> list[tuple[str, bool]]:
    base = f"{DEV_REPO}/{ARTIFACT_PATH}/{version}/kotlin-native-prebuilt-"
    names = [tpl.format(v=version) for tpl in PLATFORM_FILES]
    names.append(f"{version}.pom")
    urls = [base + n for n in names]
    with ThreadPoolExecutor(max_workers=len(urls)) as pool:
        results = list(pool.map(artifact_exists, urls))
    return list(zip(names, results))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--version", help="verify this exact version instead of resolving the newest one")
    parser.add_argument("--repo-root", default=".", help="path to the Kotlin repository root (default: cwd)")
    parser.add_argument("--json", action="store_true", help="print only the JSON result")
    args = parser.parse_args()

    root = Path(args.repo_root).resolve()
    props = root / "gradle.properties"
    current = read_property(props, "kotlin.native.version.default")
    snapshot = read_property(props, "defaultSnapshotVersion")

    out = sys.stderr if args.json else sys.stdout

    def say(msg: str = "") -> None:
        print(msg, file=out)

    problems: list[str] = []
    build: dict | None = None

    if args.version:
        target = args.version
        builds = latest_master_builds(50)
        match = next((b for b in builds if b.get("number") == target), None)
        if match:
            build = match
        else:
            problems.append(
                f"{target} is not among the last 50 successful master builds of "
                f"Kotlin_KotlinDev_KotlinNativePublishMaven. It may be from a feature branch, "
                f"a failed build, or too old. Confirm manually before using it."
            )
    else:
        builds = latest_master_builds(1)
        if not builds:
            raise SystemExit("error: TeamCity returned no successful master builds")
        build = builds[0]
        target = build["number"]

    say(f"Target version : {target}")
    say(f"Current value  : {current or '(kotlin.native.version.default not found in gradle.properties)'}")
    say(f"Snapshot line  : {snapshot or '(defaultSnapshotVersion not found)'}")
    say()

    if build:
        say("TeamCity (Kotlin_KotlinDev_KotlinNativePublishMaven)")
        say(f"  branch   : {build.get('branchName')}")
        say(f"  status   : {build.get('status')}")
        say(f"  finished : {build.get('finishDate')}")
        say(f"  build    : {build.get('webUrl')}")
        if build.get("branchName") != "master":
            problems.append(f"build is from branch {build.get('branchName')!r}, not master")
        say()

    say(f"Artifacts in {DEV_REPO}")
    for name, ok in verify_artifacts(target):
        say(f"  [{'ok' if ok else 'MISSING'}] kotlin-native-prebuilt-{name}")
        if not ok:
            problems.append(f"artifact kotlin-native-prebuilt-{name} is missing from the dev repository")
    say()

    # A bump must stay on the same X.Y line as the repo's own snapshot version. Crossing lines
    # means KGP would ship against a K/N from a different release train.
    if snapshot:
        want = version_line(snapshot)
        got = version_line(target)
        if want and got and want != got:
            problems.append(
                f"version line mismatch: repo builds {snapshot} ({want}.x) but {target} is on {got}.x"
            )

    if current:
        cur_n, new_n = dev_number(current), dev_number(target)
        if current == target:
            problems.append(f"gradle.properties already says {target} — nothing to bump")
        elif cur_n is not None and new_n is not None and new_n < cur_n:
            problems.append(f"{target} is older than the current {current} — this is a downgrade")

    if problems:
        say("PROBLEMS")
        for p in problems:
            say(f"  - {p}")
    else:
        say(f"OK — safe to set kotlin.native.version.default={target}")

    result = {
        "version": target,
        "current": current,
        "default_snapshot_version": snapshot,
        "teamcity_build": build,
        "problems": problems,
        "ok": not problems,
    }
    print(json.dumps(result, indent=2))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main())
