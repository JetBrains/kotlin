#!/usr/bin/env bash
set -euo pipefail

# Air runs this from a materialized copy, so use the checkout working directory.
cd "$(git rev-parse --show-toplevel)"
umask 022

toolchains="$HOME/.local/share/air-kotlin"
jdk17="$toolchains/temurin-17.0.18+8"
if [[ ! -x "$jdk17/bin/javac" ]]; then
    echo "Installing Temurin JDK 17 for Kotlin's Gradle build helpers..."
    [[ "$(uname -m)" == x86_64 ]] || { echo "This setup requires Linux x86_64." >&2; exit 1; }
    mkdir -p "$toolchains"
    staging="$(mktemp -d "$toolchains/install.XXXXXX")"
    trap 'rm -rf "$staging"' EXIT
    curl --fail --location --show-error --proxy "${HTTPS_PROXY:?Air HTTPS proxy is required}" \
        https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.18%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.18_8.tar.gz \
        --output "$staging/jdk.tar.gz"
    echo "0c94cbb54325c40dcf026143eb621562017db5525727f2d9131a11250f72c450  $staging/jdk.tar.gz" | sha256sum --check
    mkdir "$staging/jdk"
    tar -xzf "$staging/jdk.tar.gz" --strip-components=1 -C "$staging/jdk"
    mv "$staging/jdk" "$jdk17"
    rm -rf "$staging"
    trap - EXIT
fi

# Java does not read HTTP(S)_PROXY. Persist settings for later agent shells,
# including included builds that cannot provision their own bootstrap JDK.
gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$gradle_home"
properties="$gradle_home/gradle.properties"
touch "$properties"
temporary="$(mktemp "$gradle_home/air-properties.XXXXXX")"
awk '/^# BEGIN Air Kotlin setup$/ { skip=1; next }
     /^# END Air Kotlin setup$/ { skip=0; next }
     !skip { print }' "$properties" > "$temporary"
proxy_address="${HTTPS_PROXY:?Air HTTPS proxy is required}"
proxy_address="${proxy_address#*://}"
proxy_address="${proxy_address%/}"
proxy_host="${proxy_address%:*}"
proxy_port="${proxy_address##*:}"
{
    echo '# BEGIN Air Kotlin setup'
    printf 'systemProp.https.proxyHost=%s\nsystemProp.https.proxyPort=%s\n' "$proxy_host" "$proxy_port"
    printf 'systemProp.http.proxyHost=%s\nsystemProp.http.proxyPort=%s\n' "$proxy_host" "$proxy_port"
    printf 'org.gradle.java.installations.paths=%s\n' "$jdk17"
    # Bound parallel compiler work on the standard development instance.
    echo 'org.gradle.workers.max=4'
    echo '# END Air Kotlin setup'
} >> "$temporary"
mv "$temporary" "$properties"

healthcheck() {
    echo 'Warming Gradle, toolchain, dependency and compiler-module build caches...'
    bash ./gradlew :compiler:config.jvm:compileKotlin --no-daemon --console=plain &
    build_pid=$!
    while kill -0 "$build_pid" 2>/dev/null; do
        echo 'Kotlin environment check is still running...'
        sleep 20
    done
    if ! wait "$build_pid"; then
        echo 'Kotlin compiler-module build failed; see Gradle output above.' >&2
        return 1
    fi
    echo 'Kotlin environment check passed.'
}

if [[ "${AIR_STARTUP_MODE:-task}" == warmup ]]; then
    healthcheck
fi
echo 'Kotlin development environment is ready.'
