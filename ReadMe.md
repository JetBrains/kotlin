[![official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_KotlinPublic_Compiler.svg)](https://teamcity.jetbrains.com/buildConfiguration/Kotlin_KotlinPublic_Compiler?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.jetbrains.com/scans?search.rootProjectNames=Kotlin)

# Kotlin Programming Language

Welcome to [Kotlin](https://kotlinlang.org/)!   
It is an open-source, statically typed programming language supported and developed by [JetBrains](https://www.jetbrains.com/) and open-source contributors.

Some handy links:

 * [Kotlin Site](https://kotlinlang.org/)
 * [Getting Started Guide](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Try Kotlin](https://play.kotlinlang.org/)
 * [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
 * [Issue Tracker](https://youtrack.jetbrains.com/issues/KT)
 * [Kotlin YouTube Channel](https://www.youtube.com/channel/UCP7uiEZIqci43m22KDl0sNw)
 * [Forum](https://discuss.kotlinlang.org/)
 * [Kotlin Blog](https://blog.jetbrains.com/kotlin/)
 * [Subscribe to Kotlin YouTube channel](https://www.youtube.com/channel/UCP7uiEZIqci43m22KDl0sNw)
 * [Follow Kotlin on Twitter](https://twitter.com/kotlin)
 * [Public Slack channel](https://slack.kotlinlang.org/)
 * [TeamCity CI build](https://teamcity.jetbrains.com/project.html?tab=projectOverview&projectId=Kotlin)

## Kotlin Multiplatform capabilities

Support for multiplatform programming is one of Kotlin’s key benefits. It reduces time spent writing and maintaining the same code for [different platforms](https://kotlinlang.org/docs/reference/mpp-supported-platforms.html) while retaining the flexibility and benefits of native programming.

 * [Kotlin Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) for sharing code between Android and iOS
 * [Getting Started with Kotlin Multiplatform Mobile Guide](https://kotlinlang.org/docs/mobile/create-first-app.html)
 * [Kotlin Multiplatform Benefits](https://kotlinlang.org/docs/reference/multiplatform.html)
 * [Share code on all platforms](https://kotlinlang.org/docs/reference/mpp-share-on-platforms.html#share-code-on-all-platforms)
 * [Share code on similar platforms](https://kotlinlang.org/docs/reference/mpp-share-on-platforms.html#share-code-on-similar-platforms)

## Editing Kotlin

 * [Kotlin IntelliJ IDEA Plugin](https://kotlinlang.org/docs/tutorials/getting-started.html) ([source code](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin))
 * [Kotlin Eclipse Plugin](https://kotlinlang.org/docs/tutorials/getting-started-eclipse.html)
 * [Kotlin Sublime Text Package](https://github.com/vkostyukov/kotlin-sublime-package)

## Build environment requirements

This repository is using [Gradle toolchains](https://docs.gradle.org/current/userguide/toolchains.html) feature
to select and auto-provision required JDKs from [AdoptOpenJdk](https://adoptopenjdk.net) project. 

Unfortunately [AdoptOpenJdk](https://adoptopenjdk.net) project does not provide required JDK 1.6 and 1.7 images,
so you could either download them manually and provide path to installation via `JDK_1_6` and `JDK_1_7` environment variables or
use following SDK managers:
- [Asdf-vm](https://asdf-vm.com/)
- [Jabba](https://github.com/shyiko/jabba)
- [SDKMAN!](https://sdkman.io/)

Alternatively, it is still possible to only provide required JDKs via environment variables 
(see [gradle.properties](./gradle.properties#L5) for supported variable names). To ensure Gradle uses only JDKs 
from environmental variables - disable Gradle toolchain auto-detection by passing `-Porg.gradle.java.installations.auto-detect=false` option
(or put it into `$GRADLE_USER_HOME/gradle.properties`).

For local development, if you're not working on the standard library, it's OK to avoid installing JDK 1.6 and JDK 1.7.
Add `kotlin.build.isObsoleteJdkOverrideEnabled=true` to the `local.properties` file, so build will only use JDK 1.8+. Note, that in this
case, build will have Gradle remote build cache misses for some tasks. 

Note: The JDK 6 for MacOS is not available on Oracle's site. You can install it by

```bash
$ brew tap homebrew/cask-versions
$ brew install --cask java6
```

On Windows you might need to add long paths setting to the repo:

    git config core.longpaths true 

## Building

The project is built with Gradle. Run Gradle to build the project and to run the tests 
using the following command on Unix/macOS:

    ./gradlew <tasks-and-options>
    
or the following command on Windows:

    gradlew <tasks-and-options>

On the first project configuration gradle will download and setup the dependencies on

* `intellij-core` is a part of command line compiler and contains only necessary APIs.
* `idea-full` is a full blown IntelliJ IDEA Community Edition to be used in the plugin module.

These dependencies are quite large, so depending on the quality of your internet connection 
you might face timeouts getting them. In this case, you can increase timeout by specifying the following 
command line parameters on the first run: 
    
    ./gradlew -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000

## Important gradle tasks

- `clean` - clean build results
- `dist` - assembles the compiler distribution into `dist/kotlinc/` folder
- `install` - build and install all public artifacts into local maven repository
- `coreLibsTest` - build and run stdlib, reflect and kotlin-test tests
- `gradlePluginTest` - build and run gradle plugin tests
- `compilerTest` - build and run all compiler tests

To reproduce TeamCity build use `-Pteamcity=true` flag. Local builds don't run proguard and have jar compression disabled by default.

**OPTIONAL:** Some artifacts, mainly Maven plugin ones, are built separately with Maven.
Refer to [libraries/ReadMe.md](libraries/ReadMe.md) for details.

To build Kotlin/Native, see
[kotlin-native/README.md](kotlin-native/README.md#building-from-source).

## <a name="working-in-idea"></a> Working with the project in IntelliJ IDEA

Working with the Kotlin project requires at least IntelliJ IDEA 2019.1. You can download IntelliJ IDEA 2019.1 [here](https://www.jetbrains.com/idea/download).

After cloning the project, to import the project in IntelliJ choose the project directory in the Open project dialog. Then, after project opened, select 
`File` -> `New` -> `Module from Existing Sources...` in the menu, and select `build.gradle.kts` file in the project's root folder.

In the import dialog, select `use default gradle wrapper`.

To be able to run tests from IntelliJ easily, check `Delegate IDE build/run actions to Gradle` and choose `Gradle Test Runner` in the Gradle runner settings after importing the project.

At this time, you can use the latest released `1.6.x` version of the Kotlin plugin for working with the code. To make sure you have the latest version installed, use `Tools` -> `Kotlin` -> `Configure Kotlin Plugin Updates`.

For handy work with compiler tests it's recommended to use [
Kotlin Compiler Test Helper](https://github.com/demiurg906/test-data-helper-plugin)

### Dependency verification

We have a [dependencies verification](https://docs.gradle.org/current/userguide/dependency_verification.html) feature enabled in the
repository for all Gradle builds. Gradle will check hashes (md5 and sha256) of used dependencies and will fail builds with
`Dependency verification failed` errors when local artifacts are absent or have different hashes listed in the
[verification-metadata.xml](https://github.com/JetBrains/kotlin/blob/master/gradle/verification-metadata.xml) file.

It's expected that `verification-metadata.xml` should only be updated with the commits that modify the build. There are some tips how
to perform such updates:

- Use auto-generation for getting an initial list of new hashes (verify updates relate to you changes).

`./gradlew -i --write-verification-metadata sha256,md5 -Pkotlin.native.enabled=true help`

*(any other task may be used instead of `help`)*

- Consider removing old versions from the file if you are updating dependencies.
- Leave meaningful `origin` attribute (instead of `Generated by Gradle`) if you did some manual verification of the artifact.
- Always do manual verification if several hashes are needed, and a new `also-trust` tag has to be added.
- If you’re adding a dependency with OS mentioning in an artifact name (`darwin`, `mac`, `osx`, `linux`, `windows`), remember to add 
  counterparts for other platforms.

## Using -dev and -SNAPSHOT versions

We publish `-dev` and `-SNAPSHOT` versions frequently.

For `-dev` versions you can use the [list of available versions](https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/org/jetbrains/kotlin/kotlin-compiler/maven-metadata.xml) and include this maven repository:

`maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }`

For `-SNAPSHOT` versions that are updated daily, you can use the [list of available versions](https://oss.sonatype.org/content/repositories/snapshots/org/jetbrains/kotlin/kotlin-compiler/maven-metadata.xml) and include this maven repository:

`maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }`

# License
Kotlin is distributed under the terms of the Apache License (Version 2.0). See [license folder](license/README.md) for details.

# Contributing

Please be sure to review Kotlin's [contributing guidelines](docs/contributing.md) to learn how to help the project.
