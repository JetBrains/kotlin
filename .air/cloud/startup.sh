#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
echo "Preparing Kotlin's Gradle development environment"

# The included bootstrap build needs JDK 17 before toolchain provisioning
# is available. Install it in userspace; Gradle provisions other JDKs.
export JDK_17_0="$HOME/.local/share/kotlin-jdks/17"
if [ ! -x "$JDK_17_0/bin/javac" ]; then
    echo "Downloading bootstrap JDK 17"
    case "$(uname -m)" in
        x86_64) arch=x64 ;;
        aarch64) arch=aarch64 ;;
        *) echo "Unsupported JDK architecture" >&2; exit 1 ;;
    esac
    archive=$(mktemp)
    curl -fL --proxy "$HTTPS_PROXY" \
        "https://api.adoptium.net/v3/binary/latest/17/ga/linux/$arch/jdk/hotspot/normal/eclipse" \
        -o "$archive"
    mkdir -p "$JDK_17_0"
    tar -xzf "$archive" --strip-components=1 -C "$JDK_17_0"
    rm "$archive"
fi

# Included builds do not all declare installations.fromEnv.
python3 - <<'PY'
import os
from pathlib import Path
path = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'gradle.properties'
path.parent.mkdir(parents=True, exist_ok=True)
lines = path.read_text().splitlines() if path.exists() else []
key = 'org.gradle.java.installations.paths='
jdk = str(Path.home() / '.local/share/kotlin-jdks/17')
for i, line in enumerate(lines):
    if line.startswith(key):
        paths = line[len(key):].split(',')
        if jdk not in paths:
            lines[i] = line + ',' + jdk
        break
else:
    lines.append(key + jdk)
consent = 'kotlin.build.internal.gradle.setup.consent='
if not any(line.startswith(consent) for line in lines):
    lines.append(consent + 'false')
path.write_text('\n'.join(lines) + '\n')
PY

# Java does not use HTTP(S)_PROXY automatically. Keep the settings available
# to both the wrapper and toolchain JVMs, including future task shells.
mkdir -p "$HOME/.config/air"
python3 - <<'PY'
import os
import pathlib
import shlex
import urllib.parse

options = []
for scheme in ('http', 'https'):
    value = os.environ.get(scheme.upper() + '_PROXY')
    if value:
        proxy = urllib.parse.urlsplit(value)
        if not proxy.hostname or not proxy.port:
            raise SystemExit('Expected a proxy URL with a host and port')
        options.extend((f'-D{scheme}.proxyHost={proxy.hostname}',
                        f'-D{scheme}.proxyPort={proxy.port}'))
if options:
    options.append('-Dhttp.nonProxyHosts=localhost|127.0.0.1')
path = pathlib.Path.home() / '.config/air/kotlin-env.sh'
path.write_text('export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-}"' +
                shlex.quote(' ' + ' '.join(options)) + '\n' +
                'export JDK_17_0="$HOME/.local/share/kotlin-jdks/17"\n')
PY
hook='[ ! -f "$HOME/.config/air/kotlin-env.sh" ] || . "$HOME/.config/air/kotlin-env.sh" # air-kotlin-env'
profile="$HOME/.profile"
for candidate in "$HOME/.bash_profile" "$HOME/.bash_login" "$HOME/.profile"; do
    if [ -f "$candidate" ]; then
        profile="$candidate"
        break
    fi
done
for shell_file in "$profile" "$HOME/.bashrc"; do
    if ! grep -q ' # air-kotlin-env$' "$shell_file" 2>/dev/null; then
        printf '\n%s\n' "$hook" >> "$shell_file"
    fi
done
. "$HOME/.config/air/kotlin-env.sh"

healthcheck() {
    echo "Checking Gradle configuration and compiling kotlin-util-io (cold builds download JDKs and dependencies)"
    bash ./gradlew :kotlin-util-io:compileKotlin --console=plain &
    local build_pid=$!
    while kill -0 "$build_pid" 2>/dev/null; do
        echo "Kotlin build smoke check is running..."
        sleep 15
    done
    if ! wait "$build_pid"; then
        echo "Kotlin build smoke check failed; inspect the Gradle output above" >&2
        return 1
    fi
    echo "Kotlin development environment is ready"
}

if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then
    healthcheck
fi
