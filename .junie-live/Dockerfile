# syntax=docker/dockerfile:1
#
# One image for BOTH roles (proxy + agent). Based on the Kotlin build env so the
# agent can build kotlin; the proxy just reuses the same image.
#
# Install path mirrors the Junie Live GHA workflow: download the RELEASE binary
# (no building from source), then unpack + raise the embedded Python/Hermes
# runtime, then Junie CLI nightly.
#
# The Anthropic Sandbox Runtime step is intentionally DROPPED: we run
# --use-sandbox=false inside a GHA container, so SRT (bubblewrap/node/socat/srt)
# is neither installable nor needed.

FROM registry.jetbrains.team/p/kt/containers-public/kotlin-build-env:v48

# "latest" resolves to the newest published release; pin a vX.Y here to freeze it.
ARG JUNIE_LIVE_VERSION=latest

# Fixed runtime locations (the workflow computes these per-run; here they are static).
ENV JL_RUNTIME_DIR=/opt/junie-live/runtime \
    JL_RUNTIME_BIN_DIR=/opt/junie-live/bin \
    PATH=/opt/junie-live/bin:/opt/junie-live/cli:/root/.local/bin:${PATH}

# RUN defaults to /bin/sh (dash) which lacks `set -o pipefail`; use bash.
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN set -euo pipefail; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates git procps; \
    rm -rf /var/lib/apt/lists/*; \
    \
    # 1) Resolve release tag and download the junie-live orchestrator binary.
    release_tag="$JUNIE_LIVE_VERSION"; \
    if [ -z "$release_tag" ] || [ "$release_tag" = "latest" ]; then \
      release_url="$(curl -fsSLI -o /dev/null -w '%{url_effective}' \
        https://github.com/JetBrains/junie-live/releases/latest)"; \
      release_tag="${release_url##*/}"; \
    fi; \
    [ -n "$release_tag" ] || { echo "could not resolve the Junie Live release" >&2; exit 1; }; \
    case "$(uname -m)" in \
      x86_64|amd64) architecture=amd64 ;; \
      arm64|aarch64) architecture=arm64 ;; \
      *) echo "unsupported architecture: $(uname -m)" >&2; exit 1 ;; \
    esac; \
    mkdir -p /opt/junie-live/cli "$JL_RUNTIME_DIR" "$JL_RUNTIME_BIN_DIR"; \
    curl -fsSL --retry 3 \
      -o /opt/junie-live/cli/junie-live \
      "https://github.com/JetBrains/junie-live/releases/download/$release_tag/junie-live-linux-$architecture"; \
    chmod 0755 /opt/junie-live/cli/junie-live; \
    /opt/junie-live/cli/junie-live --version; \
    \
    # 2) Unpack the embedded Python-runtime sources (.hermes-rev, pyproject.toml, uv.lock, setup script).
    junie-live unpack-runtime --dest "$JL_RUNTIME_DIR"; \
    test -f "$JL_RUNTIME_DIR/pyproject.toml"; \
    test -f "$JL_RUNTIME_DIR/uv.lock"; \
    test -f "$JL_RUNTIME_DIR/scripts/setup-junie-live-python-runtime.sh"; \
    \
    # 3) Raise the uv project: hermes-agent[all,slack,anthropic] + slack-sdk, pinned by uv.lock.
    curl -LsSf https://astral.sh/uv/0.11.6/install.sh | env UV_NO_MODIFY_PATH=1 sh; \
    export PATH="/root/.local/bin:$PATH"; \
    bash "$JL_RUNTIME_DIR/scripts/setup-junie-live-python-runtime.sh" \
      --junie-live-root "$JL_RUNTIME_DIR" \
      --bin-dir "$JL_RUNTIME_BIN_DIR" \
      --no-dev; \
    test -x "$JL_RUNTIME_DIR/.venv/bin/python"; \
    test -x "$JL_RUNTIME_BIN_DIR/hermes"; \
    "$JL_RUNTIME_DIR/.venv/bin/python" -c \
      'import hermes_cli, slack_sdk, slack_bolt, anthropic; print("imports ok")'; \
    \
    # 4) Junie CLI (the senior-dev worker), nightly channel.
    curl -fsSL https://junie.jetbrains.com/install-nightly.sh | bash; \
    \
    # Sanity: all three entrypoints resolve on PATH.
    junie-live --version; hermes --version; junie --version
