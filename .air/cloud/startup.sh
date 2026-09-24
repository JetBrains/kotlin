#!/usr/bin/env bash
# Environment bootstrap for the Kotlin repository.
#
# Two modes, selected by AIR_STARTUP_MODE:
#   warmup - snapshot-baking run: install toolchains, prime Gradle caches, build the
#            compiler distribution and block on `healthcheck` at the end.
#   task   - real task run: make sure the toolchains and shell environment are in place,
#            then return quickly (the heavy work is already in the snapshot).

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$HOME/.air-kotlin-env.sh"
JDK_DIR="$HOME/.jdks"
MARKER="# >>> air kotlin env >>>"

if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then WARMUP=1; else WARMUP=; fi

PROXY_HOST=""
PROXY_PORT=""

log() { printf '[startup %s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
fail() { printf '[startup %s] ERROR: %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; exit 1; }

# Adoptium (Temurin) major versions this build needs as Gradle toolchains.
# Derived from gradle.properties (org.gradle.java.installations.fromEnv) and
# repo/gradle-build-conventions/utilities/src/main/kotlin/JvmToolchain.kt (JdkMajorVersion):
#   8  - DEFAULT_JVM_TOOLCHAIN / DEFAULT_JVM_TARGET
#   11 - DEFAULT_JAVA_LAUNCHER_FOR_TESTS
#   17 - build logic (repo/gradle-settings-conventions, Gradle plugin modules)
#   21 - Gradle daemon (gradle/gradle-daemon-jvm.properties)
#   25 - tests with Java 25, native/unsafe-mem, test-instrumenter
# JDK 9 appears in JdkMajorVersion only as a bytecode target (-Xjdk-release), never as a toolchain.
JDK_MAJORS=(8 11 17 21 25)
DAEMON_JDK=21

# Maps a major version to the JDK_* environment variable name gradle.properties reads
# through org.gradle.java.installations.fromEnv.
jdk_env_name() {
    case "$1" in
        8) echo "JDK_1_8" ;;
        *) echo "JDK_${1}_0" ;;
    esac
}

# ---------------------------------------------------------------------------
# 1. JVM proxy configuration
# ---------------------------------------------------------------------------
# Outbound traffic leaves this environment only through the HTTP(S) proxy, and the JVM
# ignores the http_proxy/https_proxy variables that curl honours, so Gradle needs the
# proxy as system properties. They go into GRADLE_OPTS (the wrapper JVM, which downloads
# the distribution) and into GRADLE_USER_HOME/gradle.properties (the daemon, which
# resolves dependencies and provisions toolchains) - deliberately NOT into
# JAVA_TOOL_OPTIONS: that would make every JVM in the environment print
# "Picked up JAVA_TOOL_OPTIONS ..." on stderr, which pollutes the output of compiler
# tests that compare subprocess output.
configure_proxy() {
    local raw="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
    if [ -z "$raw" ]; then
        log "no HTTP(S) proxy configured; using direct connections"
        return 0
    fi

    local hostport="${raw#*://}"
    hostport="${hostport%/}"
    PROXY_HOST="${hostport%%:*}"
    PROXY_PORT="${hostport##*:}"
    [ -n "$PROXY_HOST" ] && [ -n "$PROXY_PORT" ] && [ "$PROXY_HOST" != "$PROXY_PORT" ] \
        || fail "cannot parse a host:port proxy out of '$raw'"
    log "JVM proxy: $PROXY_HOST:$PROXY_PORT"
}

proxy_java_opts() {
    [ -n "$PROXY_HOST" ] || return 0
    printf -- '-Dhttp.proxyHost=%s -Dhttp.proxyPort=%s -Dhttps.proxyHost=%s -Dhttps.proxyPort=%s -Dhttp.nonProxyHosts=localhost|127.0.0.1' \
        "$PROXY_HOST" "$PROXY_PORT" "$PROXY_HOST" "$PROXY_PORT"
}

# ---------------------------------------------------------------------------
# 2. JDK toolchains
# ---------------------------------------------------------------------------
# Gradle's foojay auto-provisioning is wired into the root build only, while the
# included build-logic builds under repo/ are configured earlier and fail outright
# without a locally visible JDK 17. So install the JDKs here into ~/.jdks, which Gradle
# auto-detects, and also export the JDK_* variables gradle.properties reads.
install_jdk() {
    local major="$1"
    local target="$JDK_DIR/temurin-$major"

    if [ -x "$target/bin/javac" ]; then
        log "JDK $major already installed at $target"
        return 0
    fi

    local url="https://api.adoptium.net/v3/binary/latest/$major/ga/linux/x64/jdk/hotspot/normal/eclipse"
    local tarball="$JDK_DIR/temurin-$major.tar.gz"
    log "downloading Temurin JDK $major"
    curl -fsSL --retry 5 --retry-all-errors --retry-delay 5 --connect-timeout 30 -o "$tarball" "$url" \
        || fail "failed to download JDK $major from $url"

    rm -rf "$target.tmp" && mkdir -p "$target.tmp"
    tar -xzf "$tarball" -C "$target.tmp" --strip-components=1 || fail "failed to unpack JDK $major"
    rm -f "$tarball"
    rm -rf "$target" && mv "$target.tmp" "$target"
    [ -x "$target/bin/javac" ] || fail "JDK $major unpacked without bin/javac"
    log "installed JDK $major: $("$target/bin/java" -version 2>&1 | head -1)"
}

install_jdks() {
    mkdir -p "$JDK_DIR"
    local major
    for major in "${JDK_MAJORS[@]}"; do
        install_jdk "$major"
    done
}

# ---------------------------------------------------------------------------
# 3. Shell environment
# ---------------------------------------------------------------------------
# The agent gets a fresh login shell once this script exits, so exports made here are
# lost. Write them into an env file and source that file from the login shell profile
# and from ~/.bashrc, guarded by a marker so repeated runs cannot duplicate the hook.
write_env_file() {
    log "writing $ENV_FILE"
    {
        echo "# Generated by .air/cloud/startup.sh - environment for building the Kotlin repository."
        if [ -n "$PROXY_HOST" ]; then
            echo "# Gradle's launcher JVM needs the egress proxy as system properties; add them once."
            echo "case \"\${GRADLE_OPTS:-}\" in"
            echo "    *-Dhttps.proxyHost=$PROXY_HOST*) ;;"
            echo "    *) export GRADLE_OPTS=\"\${GRADLE_OPTS:-} $(proxy_java_opts)\" ;;"
            echo "esac"
        fi
        local major env_name
        for major in "${JDK_MAJORS[@]}"; do
            env_name="$(jdk_env_name "$major")"
            echo "export $env_name=\"$JDK_DIR/temurin-$major\""
        done
        # Build with a real JDK instead of the JetBrains Runtime the workspace image ships.
        echo "export JAVA_HOME=\"$JDK_DIR/temurin-$DAEMON_JDK\""
        echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    } > "$ENV_FILE"

    local hook="$MARKER
[ -f \"$ENV_FILE\" ] && . \"$ENV_FILE\"
# <<< air kotlin env <<<"

    local profile="" candidate
    for candidate in "$HOME/.bash_profile" "$HOME/.bash_login" "$HOME/.profile"; do
        if [ -f "$candidate" ]; then profile="$candidate"; break; fi
    done
    [ -n "$profile" ] || { profile="$HOME/.profile"; : > "$profile"; }

    for candidate in "$profile" "$HOME/.bashrc"; do
        [ -f "$candidate" ] || : > "$candidate"
        if ! grep -qF "$MARKER" "$candidate"; then
            printf '\n%s\n' "$hook" >> "$candidate"
            log "hooked $ENV_FILE into $candidate"
        fi
    done
}

# ---------------------------------------------------------------------------
# 4. Gradle user home configuration
# ---------------------------------------------------------------------------
# GRADLE_USER_HOME/gradle.properties wins over the project's own gradle.properties, so
# keep it to settings about *this machine* rather than about the build itself.
configure_gradle_home() {
    local gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
    mkdir -p "$gradle_home"
    local props="$gradle_home/gradle.properties"
    log "writing $props"
    {
        echo "# Generated by .air/cloud/startup.sh"
        if [ -n "$PROXY_HOST" ]; then
            echo "systemProp.http.proxyHost=$PROXY_HOST"
            echo "systemProp.http.proxyPort=$PROXY_PORT"
            echo "systemProp.https.proxyHost=$PROXY_HOST"
            echo "systemProp.https.proxyPort=$PROXY_PORT"
            echo "systemProp.http.nonProxyHosts=localhost|127.0.0.1"
        fi
        # The internal-gradle-setup settings plugin prompts for consent on stdin when it
        # can reach the JetBrains-internal properties host. Nobody is here to answer.
        echo "kotlin.build.internal.gradle.setup.consent=false"
    } > "$props"
}

# ---------------------------------------------------------------------------
# 5. Gradle warm-up
# ---------------------------------------------------------------------------
gradle_warmup() {
    cd "$REPO_DIR"
    chmod +x ./gradlew

    log "fetching the Gradle distribution"
    ./gradlew --version || fail "the Gradle wrapper could not start"

    log "building the compiler distribution: ./gradlew dist"
    log "this downloads the intellij-core/idea-full dependencies and fills the Gradle build cache"
    ./gradlew dist || fail "'./gradlew dist' failed; see the output above"
    log "compiler distribution built at $REPO_DIR/dist/kotlinc"

    # Free the daemon's heap before the health check runs; daemons are not part of the
    # snapshot anyway, only the caches they wrote to disk are.
    ./gradlew --stop || true
}

# ---------------------------------------------------------------------------
# 6. Health check
# ---------------------------------------------------------------------------
# Asserts the environment can do what a task on this repository actually needs: every
# required JDK toolchain is usable, and the compiler built from this checkout compiles
# and runs Kotlin code. It owns its own waiting and keeps polling until things are ready.
healthcheck() {
    log "healthcheck: verifying JDK toolchains"
    local major env_name home
    for major in "${JDK_MAJORS[@]}"; do
        env_name="$(jdk_env_name "$major")"
        home="$JDK_DIR/temurin-$major"
        [ -x "$home/bin/javac" ] || fail "healthcheck: $env_name -> $home has no bin/javac"
        "$home/bin/javac" -version >/dev/null 2>&1 || fail "healthcheck: javac from $home is not runnable"
    done
    log "healthcheck: JDK toolchains ${JDK_MAJORS[*]} OK"

    local kotlinc="$REPO_DIR/dist/kotlinc/bin/kotlinc-jvm"
    local stdlib="$REPO_DIR/dist/kotlinc/lib/kotlin-stdlib.jar"
    while [ ! -x "$kotlinc" ] || [ ! -f "$stdlib" ]; do
        log "healthcheck: waiting for the compiler distribution at $REPO_DIR/dist/kotlinc"
        sleep 10
    done

    local work
    work="$(mktemp -d)"
    cat > "$work/hello.kt" <<'EOF'
fun main() {
    println("kotlin-env-ok")
}
EOF

    log "healthcheck: compiling a sample program with the freshly built compiler"
    while ! JAVA_HOME="$JDK_DIR/temurin-$DAEMON_JDK" "$kotlinc" "$work/hello.kt" -d "$work/out" > "$work/kotlinc.log" 2>&1; do
        log "healthcheck: kotlinc is not usable yet; last output:"
        tail -20 "$work/kotlinc.log" || true
        sleep 10
    done

    local output
    output="$("$JDK_DIR/temurin-$DAEMON_JDK/bin/java" -cp "$work/out:$stdlib" HelloKt 2>"$work/java.log")" \
        || fail "healthcheck: running the compiled program failed: $(cat "$work/java.log")"
    [ "$output" = "kotlin-env-ok" ] || fail "healthcheck: unexpected program output: '$output'"

    rm -rf "$work"
    log "healthcheck: the compiler built from this checkout compiles and runs Kotlin code"
}

# ---------------------------------------------------------------------------
main() {
    log "mode=${AIR_STARTUP_MODE:-task} repo=$REPO_DIR"
    configure_proxy
    if [ -n "$PROXY_HOST" ]; then
        export GRADLE_OPTS="${GRADLE_OPTS:-} $(proxy_java_opts)"
    fi

    install_jdks
    write_env_file
    configure_gradle_home

    # shellcheck disable=SC1090
    . "$ENV_FILE"

    if [ -n "$WARMUP" ]; then
        gradle_warmup
        healthcheck
    else
        log "task mode: toolchains ready; skipping the warm-up build (its caches come from the snapshot)"
    fi
    log "done"
}

main "$@"
