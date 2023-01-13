# Building 1.7.20

This tutorial explains how to build release [1.7.20](https://github.com/JetBrains/kotlin/releases/tag/v1.7.20) locally.

## Prerequisites
You must have:
* Linux or MacOS.
* Docker installed.
* 14 GB memory available and configured in Docker. 

## Set environment variables

The following environment variables must be set:

```
export DEPLOY_VERSION=1.7.20
export BUILD_NUMBER=1.7.20-release-201
export KOTLIN_NATIVE_VERSION=1.7.20
export DOCKER_CONTAINER_URL=kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v5
```
## Clone Kotlin repository

In a new folder, clone the release tag from the Kotlin repository, and change directory to the build folder:

```
git clone --depth 1 --branch v$DEPLOY_VERSION https://github.com/JetBrains/kotlin.git kotlin-build-$DEPLOY_VERSION
cd kotlin-build-$DEPLOY_VERSION
```

## Build and verify the compiler

Download and save [scripts/build-kotlin-compiler.sh](https://github.com/JetBrains/kotlin/blob/1.7.20/scripts/build-kotlin-compiler.sh) to 
the`scripts` folder in your repository, and then execute it in a docker container:

```
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-compiler.sh $DEPLOY_VERSION $BUILD_NUMBER"
```

This generates a ZIP file: `dist/kotlin-compiler-1.7.20.zip`.

Check that the SHA 256 checksum is the same as the ZIP file [published on GitHub](https://github.com/JetBrains/kotlin/releases/download/v1.7.20/kotlin-compiler-1.7.20.zip):
5e3c8d0f965410ff12e90d6f8dc5df2fc09fd595a684d514616851ce7e94ae7d

## Build maven artifacts

Download and save [scripts/build-kotlin-maven.sh](https://github.com/JetBrains/kotlin/blob/1.7.20/scripts/build-kotlin-maven.sh) to 
the `scripts` folder in your repository, and then execute it in a docker container:

```
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-maven.sh $DEPLOY_VERSION $BUILD_NUMBER $KOTLIN_NATIVE_VERSION"
```

This generates a ZIP file: `build/repo-reproducible/reproducible-maven-1.7.20.zip`.

**Note:** Instructions for checking reproducibility will be covered in the upcoming release. There are already many JAR files on Maven
central that are reproducible with this tutorial.
