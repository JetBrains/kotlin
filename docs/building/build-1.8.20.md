# Building 1.8.20

This tutorial explains how to build release [1.8.20](https://github.com/JetBrains/kotlin/releases/tag/v1.8.20) locally.

## Prerequisites

You must have:
* Linux or macOS.
* Docker installed.
* 14 GB memory available and configured in Docker.
* To get reproducible artifacts, [umask](https://en.wikipedia.org/wiki/Umask) command without parameters should 
produce `0022` or `022` output.

## Set environment variables

The following environment variables must be set:

```shell
export DEPLOY_VERSION=1.8.20
export BUILD_NUMBER=1.8.20-release-327
export MAVEN_BUILD_NUMBER=1.8.20-release-327\(1.8.20\)
export KOTLIN_NATIVE_VERSION=1.8.20
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

Check that the SHA 256 checksum is equal to the published one for [kotlin-compiler.zip](https://github.com/JetBrains/kotlin/releases/download/v1.8.20/kotlin-compiler-1.8.20.zip):

`10df74c3c6e2eafd4c7a5572352d37cbe41774996e42de627023cb4c82b50ae4`

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

Check that the SHA 256 checksum is equal to 
`cab34569302361c66747691f6f04816b7dd0478e4ede7cea6420931ca953f731`.