FROM --platform=linux/amd64 eclipse-temurin:21-jdk AS builder

WORKDIR /kotlin

# Gradle perf options
ENV GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.jvmargs=-Xmx2g -Dorg.gradle.caching=true"

# Copy minimal files first to leverage Docker layer caching for dependency resolution
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle* settings.gradle*
COPY build.gradle* build.gradle*
# Note: No buildSrc directory in this repo; skip copying it to avoid build failure.
# If present, also copy top-level gradle config files:
# COPY gradle.properties gradle.properties

# Warm up wrapper and basic Gradle state (uses cache mount on same RUN)
RUN --mount=type=cache,target=/home/.gradle,id=kotlin-gradle-cache \
    chmod +x ./gradlew && ./gradlew --version

# Now copy the rest (changes here won’t invalidate earlier dependency layers)
COPY . .

# Build only what’s needed. Keep it in a single RUN so the daemon stays alive.
RUN --mount=type=cache,target=/home/.gradle,id=kotlin-gradle-cache \
    ./gradlew :kotlin-remote-daemon:installDist \
      -x test --no-scan --parallel --build-cache \
      --configuration-cache

# ---------- Runtime stage ----------
FROM --platform=linux/amd64 eclipse-temurin:21-jre AS runtime

RUN useradd -ms /bin/bash appuser
USER appuser
WORKDIR /app

COPY --from=builder /kotlin/compiler/daemon/remote-daemon/build/install/remote-daemon /app

EXPOSE 8000
ENTRYPOINT ["/app/bin/remote-daemon"]
CMD []


