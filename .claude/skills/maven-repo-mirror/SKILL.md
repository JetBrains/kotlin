---
name: maven-repo-mirror
description: |
  Manage the KGP functional test Maven repo mirror — add, remove, or list
  third-party dependencies in mavenRepoMirror.tar.gz. Use this skill whenever
  a functional test needs a new third-party dependency that isn't in the mirror,
  when a test fails with "Could not resolve" for a non-Kotlin artifact, when
  removing unused dependencies from the mirror, or when listing what's currently
  mirrored. Also triggers on mentions of mavenRepoMirror, replicate-maven-dep,
  or "stub dependency".
user-invocable: true
argument-hint: "[add|remove|list] [group:artifact:version]"
allowed-tools: Bash
paths: ["libraries/tools/kotlin-gradle-plugin/**"]
---

# Maven Repo Mirror Management

The KGP functional tests use a local Maven repo mirror instead of fetching
dependencies from the network. The mirror contains **real metadata** (POM,
Gradle Module Metadata) with **stub binaries** (empty JARs/klibs/AARs).

## Key paths

| What | Path |
|------|------|
| Archive | `libraries/tools/kotlin-gradle-plugin/src/functionalTest/resources/mavenRepoMirror.tar.gz` |
| Helper script | `libraries/tools/kotlin-gradle-plugin/scripts/replicate-maven-dep.sh` |
| Extracted at build time to | `libraries/tools/kotlin-gradle-plugin/build/mavenRepoMirror/` |

## Adding a dependency

Run the helper script from the `scripts/` directory:

```bash
cd libraries/tools/kotlin-gradle-plugin/scripts

# Single artifact
./replicate-maven-dep.sh com.example:library:1.2.3

# With all transitive dependencies
./replicate-maven-dep.sh com.example:library:1.2.3 --with-transitives
```

The script:
1. Extracts the existing `mavenRepoMirror.tar.gz` into a temp directory
2. Downloads POM and `.module` metadata from Maven Central (via cache-redirector)
3. Creates stub binaries (empty JARs, AARs with `classes.jar`, empty klibs)
4. Downloads real content for metadata JARs (needed by `IdeTransformedMetadataDependencyResolver`)
5. Follows `available-at` references in `.module` files to replicate sub-modules
6. Re-compresses the archive

After adding, **commit the updated `mavenRepoMirror.tar.gz`** — it's checked into git.

### When to use `--with-transitives`

Use it when the test resolves a full dependency graph (e.g., `configureRepositoriesForTests()`
adds `mavenCentralCacheRedirector()` which Gradle uses to resolve transitives). Skip it when
the test only needs the root artifact's metadata.

## Removing a dependency

There's no dedicated removal script. Remove manually:

```bash
cd libraries/tools/kotlin-gradle-plugin

# Extract
WORK_DIR=$(mktemp -d)
tar xzf src/functionalTest/resources/mavenRepoMirror.tar.gz -C "$WORK_DIR"

# Remove the artifact directory
# Format: mavenRepoMirror/group/path/artifact/version/
rm -rf "$WORK_DIR/mavenRepoMirror/com/example/library/1.2.3"

# If removing the entire artifact (all versions), remove the artifact dir
rm -rf "$WORK_DIR/mavenRepoMirror/com/example/library"

# Re-compress
tar czf src/functionalTest/resources/mavenRepoMirror.tar.gz -C "$WORK_DIR" mavenRepoMirror

# Cleanup
rm -rf "$WORK_DIR"
```

## Listing mirrored dependencies

```bash
# List all artifacts with versions
tar tzf libraries/tools/kotlin-gradle-plugin/src/functionalTest/resources/mavenRepoMirror.tar.gz \
  | grep '\.pom$' \
  | sed 's|mavenRepoMirror/||; s|/[^/]*\.pom$||; s|/|.|g; s|\.\([^.]*\)\.\([^.]*\)$|:\1:\2|'

# Count total artifacts
tar tzf libraries/tools/kotlin-gradle-plugin/src/functionalTest/resources/mavenRepoMirror.tar.gz \
  | grep '\.pom$' | wc -l
```

## How it fits into the build

1. `extractMavenRepoMirror` task extracts the tar.gz to `build/mavenRepoMirror/`
2. `addMavenRepoMirror()` in `buildProject.kt` injects the mirror as a Maven repo for every test project
3. The mirror repo excludes `org.jetbrains.kotlin.*` — Kotlin project artifacts come from `kotlinBuildDeps` instead
4. Tests use standard Maven coordinates — no code changes needed when adding to the mirror

## Troubleshooting

**"Could not resolve" in functional test**: Check if the dependency is in the mirror
(`tar tzf ... | grep artifact-name`). If not, add it with the script. If it's a
`org.jetbrains.kotlin` artifact, it should come from `kotlinBuildDeps` (populated by
`populateFunctionalTestRepo`), not the mirror.

**Metadata JAR has wrong content**: The script downloads real metadata JARs (for
`metadataApiElements` variants). If the content is wrong, delete the artifact from the
mirror and re-run the script.

**AAR missing classes.jar**: The script creates AAR stubs containing a `classes.jar` entry.
If AGP's artifact transform fails, verify the AAR stub is a valid ZIP with `classes.jar` inside.
