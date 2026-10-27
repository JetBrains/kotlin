#!/usr/bin/env bash
# Air cloud environment startup script for the Kotlin repository.
# AIR_STARTUP_MODE=warmup: prime Gradle/JDK/dependency caches and verify the build (snapshot-baking run).
# AIR_STARTUP_MODE=task:   only lightweight per-launch configuration; exits promptly.
set -euo pipefail

if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then WARMUP=1; else WARMUP=; fi

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_DIR"

log() { echo "[startup $(date +%H:%M:%S)] $*"; }

# --- Gradle proxy configuration -------------------------------------------------------------
# The JVM ignores HTTPS_PROXY, so Gradle (wrapper download, dependency resolution, toolchain
# provisioning) needs explicit proxy system properties in the user-level gradle.properties.
configure_gradle_proxy() {
  local props="$HOME/.gradle/gradle.properties" begin="# >>> air proxy >>>" end="# <<< air proxy <<<"
  mkdir -p "$HOME/.gradle"
  touch "$props"
  sed -i "/^$begin\$/,/^$end\$/d" "$props"
  local proxy="${HTTPS_PROXY:-${https_proxy:-}}"
  if [ -z "$proxy" ]; then
    log "no HTTPS_PROXY set; skipping Gradle proxy configuration"
    return
  fi
  local hostport="${proxy#*://}"
  hostport="${hostport#*@}"
  hostport="${hostport%%/*}"
  local host="${hostport%:*}" port="${hostport##*:}"
  {
    echo "$begin"
    for scheme in http https; do
      echo "systemProp.$scheme.proxyHost=$host"
      echo "systemProp.$scheme.proxyPort=$port"
    done
    echo "systemProp.http.nonProxyHosts=localhost|127.0.0.1|::1"
    echo "$end"
  } >> "$props"
  log "configured Gradle proxy $host:$port in $props"
}

configure_gradle_proxy
chmod +x gradlew

healthcheck() {
  log "healthcheck: configuring the Gradle build"
  ./gradlew help --console=plain
  log "healthcheck: OK"
}

if [ -n "$WARMUP" ]; then
  healthcheck
fi
log "startup finished (mode=${AIR_STARTUP_MODE:-unset})"
