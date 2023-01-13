# Building 1.8.0-Beta

This tutorial explains how to build release [1.8.0-Beta](https://github.com/JetBrains/kotlin/releases/tag/v1.8.0-Beta) locally.

## Prerequisites
You must have:
* Linux or MacOS.
* Docker installed.
* 14 GB memory available and configured in Docker. 

## Set environment variables

The following environment variables must be set:

```
export DEPLOY_VERSION=1.8.0-Beta
export BUILD_NUMBER=1.8.0-Beta-release-224
export KOTLIN_NATIVE_VERSION=1.8.0-Beta
export DOCKER_CONTAINER_URL=kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v5
```
## Clone Kotlin repository

In a new folder, clone the release tag from the Kotlin repository, and change directory to the build folder:

```
git clone --depth 1 --branch v$DEPLOY_VERSION https://github.com/JetBrains/kotlin.git kotlin-build-$DEPLOY_VERSION
cd kotlin-build-$DEPLOY_VERSION
```

## Build and verify the compiler

Download and save [scripts/build-kotlin-compiler.sh](https://github.com/JetBrains/kotlin/blob/1.8.0/scripts/build-kotlin-compiler.sh) to
the`scripts` folder in your repository, and then execute it in a docker container:

```
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-compiler.sh $DEPLOY_VERSION $BUILD_NUMBER"
```

This generates a ZIP file: `dist/kotlin-compiler-1.8.0-Beta.zip`.

Check that the SHA 256 checksum is the same as the ZIP file [published on GitHub](https://github.com/JetBrains/kotlin/releases/download/v1.8.0-Beta/kotlin-compiler-1.8.0-Beta.zip):
2c1130237c8673280f0fc4d178c26203884619d80f795caf61a51e802b6aa262

## Build maven artifacts

Download and save [scripts/build-kotlin-maven.sh](https://github.com/JetBrains/kotlin/blob/1.8.0/scripts/build-kotlin-maven.sh) to
the `scripts` folder in your repository, and then execute it in a docker container:

```
export BUILD_NUMBER=1.8.0-Beta-release-224(1.8.0-Beta)
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-maven.sh $DEPLOY_VERSION '$BUILD_NUMBER' $KOTLIN_NATIVE_VERSION"
```

This generates a ZIP file: `build/repo-reproducible/reproducible-maven-1.8.0-Beta.zip`.

**Note:** Instructions for checking reproducibility will be covered in the upcoming release. There are already many JAR files on Maven
central that are reproducible with this tutorial.
