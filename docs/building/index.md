# Kotlin builds

This tutorial explains how to build any release starting with 1.9.0-Beta locally.

For earlier releases follow the corresponding instruction:
* [1.8.20](build-1.8.20.md)
* [1.8.0](build-1.8.0.md)
* [1.8.0-Beta](build-1.8.0-Beta.md)
* [1.7.21](build-1.7.21.md)
* [1.7.20](build-1.7.20.md)

## Prerequisites

You must have:
* Linux or macOS.
* Docker installed.
* 14 GB memory available and configured in Docker.
* To get reproducible artifacts, [umask](https://en.wikipedia.org/wiki/Umask) command without parameters should
  produce `0022` or `022` output.

## Set environment variables

All artifacts are available at the [Kotlin GitHub release page](https://github.com/JetBrains/kotlin/releases)

Each `kotlin-compiler-[DEPLOY_VERSION].zip` artifact contains `build.txt` file with the `BUILD_NUMBER` value

The following environment variables must be set:

```shell
export DEPLOY_VERSION=*
export BUILD_NUMBER=*
export MAVEN_BUILD_NUMBER=BUILD_NUMBER
export KOTLIN_NATIVE_VERSION=DEPLOY_VERSION
export DOCKER_CONTAINER_URL=kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v6
```

## Clone Kotlin repository

In a new folder, clone the release tag from the Kotlin repository, and change directory to the build folder:

```shell
git clone --depth 1 --branch v$DEPLOY_VERSION https://github.com/JetBrains/kotlin.git kotlin-build-$DEPLOY_VERSION
cd kotlin-build-$DEPLOY_VERSION
```

## Build and verify the compiler

```shell
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" \
  --volume="$(pwd):/repo" \
  --user="$(id -u):$(id -g)" \
  $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-compiler.sh $DEPLOY_VERSION $BUILD_NUMBER"
```

This generates a ZIP file: `dist/kotlin-compiler-$DEPLOY_VERSION.zip`.

Check that the SHA 256 checksum is equal to the published one for `kotlin-compiler.zip`

## Build and verify maven artifacts

```shell
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" \
  --volume="$(pwd):/repo" \
  --user="$(id -u):$(id -g)" \
  $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-maven.sh $DEPLOY_VERSION '$MAVEN_BUILD_NUMBER' $KOTLIN_NATIVE_VERSION"
```

This generates a ZIP file: `build/repo-reproducible/reproducible-maven-$DEPLOY_VERSION.zip`.

Check that the SHA 256 checksum is equal to the checksum, defined in `maven-$DEPLOY_VERSION-sha256.txt`