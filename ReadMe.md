<img width="400" height="120" alt="Kotlin Full Color Logo on Black RGB" src="https://github.com/user-attachments/assets/5323ca1c-a3ab-4d4f-bc77-cefe4f8a36ff" />

<br>
<br>
<br>

[![official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_KotlinPublic_Compiler.svg)](https://teamcity.jetbrains.com/buildConfiguration/Kotlin_KotlinPublic_Compiler?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.jetbrains.com/scans?search.rootProjectNames=Kotlin)

>*Kotlin is a modern, expressive, and statically typed programming language developed by JetBrains. 
>Designed for building robust, maintainable, and performant applications, Kotlin supports multiple platforms including JVM, Android, JavaScript, WebAssembly, and Native (via LLVM), enabling true code sharing across mobile, web, desktop, and server.*


---

# 📌 Table of Contents

- [About](#about)
- [Why Kotlin?](#why-kotlin)
- [Kotlin Multiplatform capabilities](#kotlin-multiplatform-capabilities)
- [Editing Kotlin](#editing-kotlin)
- [Build environment requirements](#build-environment-requirements)
- [Building](#building)
- [Important Gradle tasks](#important-gradle-tasks)
- [Working with the project in IntelliJ IDEA](#working-with-the-project-in-intellij-idea)
- [Dependency verification](#dependency-verification)
- [Using -dev versions](#using--dev-versions)
- [Community and Support](#community-and-support)
- [License](#license)
- [Contributing](#contributing)
- [Kotlin Foundation](#kotlin-foundation)

---

# About 
Welcome to [Kotlin](https://kotlinlang.org/)!   
Kotlin is a concise multiplatform language developed by [JetBrains](https://www.jetbrains.com/) and [contributors](https://kotlinlang.org/docs/contribute.html).

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
 * [Kotlin Foundation](https://kotlinfoundation.org/)

## Why Kotlin?

Kotlin stands out as a modern programming language that combines clarity, expressiveness, and robust engineering practices. Its design philosophy is focused on developer productivity, safety, and seamless integration with existing ecosystems.

- **Full Java Interoperability**  
  Kotlin is 100% interoperable with Java, allowing teams to adopt it gradually in existing codebases. Developers can use all existing Java libraries, frameworks, and tools without any modification.

- **Multiplatform Development**  
  Kotlin supports true multiplatform development, enabling code sharing across Android, iOS, JVM, JavaScript, WebAssembly, and native platforms through Kotlin Multiplatform. This significantly reduces duplication and improves maintainability.

- **Concise and Readable Syntax**  
  Kotlin reduces boilerplate code through smart language design, making codebases more compact, readable, and easier to maintain. Common programming patterns are supported natively in the language, resulting in cleaner code.

- **Strong Type System and Null Safety**  
  Kotlin’s type system helps eliminate entire classes of bugs, especially null pointer exceptions. Null safety is built into the type system, making your applications more stable and secure.

- **Tooling and IDE Support**  
  Developed by JetBrains, Kotlin has first-class support in IntelliJ IDEA and Android Studio. It benefits from deep tooling integration, including code completion, refactoring, debugging, linting, and more.

- **Modern Language Features**  
  Kotlin includes powerful features such as extension functions, coroutines for asynchronous programming, sealed classes, data classes, smart casts, and inline functions—providing developers with expressive and flexible language constructs.

- **Open Source and Backed by JetBrains**  
  Kotlin is an open-source language governed by the Kotlin Foundation. It’s actively developed and maintained by JetBrains with contributions from a vibrant and growing global community.

- **Safe and Production-Ready**  
  Kotlin is battle-tested in production by companies like Google, Pinterest, Netflix, Uber, and Coursera. It is the preferred language for Android app development and is used at scale in backend, web, and cross-platform applications.

- **Actively Evolving Ecosystem**  
  The Kotlin ecosystem is growing rapidly, with tools, libraries, and frameworks emerging to support everything from web development (e.g., Ktor, kotlinx.html) to data science, server-side applications, and native development.


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

Alternatively, it is still possible to only provide required JDKs via environment variables 
(see [gradle.properties](./gradle.properties#L5) for supported variable names). To ensure Gradle uses only JDKs 
from environmental variables - disable Gradle toolchain auto-detection by passing `-Porg.gradle.java.installations.auto-detect=false` option
(or put it into `$GRADLE_USER_HOME/gradle.properties`).

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

It is recommended to use the latest released version of Intellij IDEA (Community or Ultimate Edition). You can download IntelliJ IDEA [here](https://www.jetbrains.com/idea/download).

After cloning the project, import the project in IntelliJ by choosing the project directory in the Open project dialog.

For handy work with compiler tests it's recommended to use [
Kotlin Compiler Test Helper](https://github.com/demiurg906/test-data-helper-plugin)

### Dependency verification

We have a [dependencies verification](https://docs.gradle.org/current/userguide/dependency_verification.html) feature enabled in the
repository for all Gradle builds. Gradle will check hashes (md5 and sha256) of used dependencies and will fail builds with
`Dependency verification failed` errors when local artifacts are absent or have different hashes listed in the
[verification-metadata.xml](https://github.com/JetBrains/kotlin/blob/master/gradle/verification-metadata.xml) file.

It's expected that `verification-metadata.xml` should only be updated with the commits that modify the build. There are some tips how
to perform such updates:

- Delete `components` section of `verification-metadata.xml` to avoid stockpiling of old unused dependencies. You may use the following command:
```bash
#macOS
sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
#Linux & Git for Windows
sed -i -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
```
- Re-generate dependencies with Gradle's `--write-verification-metadata` command (verify update relates to your changes)

```bash
./gradlew --write-verification-metadata sha256,md5 -Pkotlin.native.enabled=true resolveDependencies
```

*`resolveDependencies` task resolves dependencies for all platforms including dependencies downloaded by plugins.*

You can also use `./scripts/update-verification-metadata.sh` script which includes both of these steps

Keep in mind:

- If you’re adding a dependency with OS mentioned in an artifact name (`darwin`, `mac`, `osx`, `linux`, `windows`), remember to add them to 
  `implicitDependencies` configuration or update `resolveDependencies` task if needed. `resolveDependencies` should resolve all dependencies
  including dependencies for different platforms.
- If you have a `local.properties` file in your Kotlin project folder, make sure that it doesn't contain `kotlin.native.enabled=false`.
  Otherwise, native-only dependencies may not be added to the verification metadata. This is because `local.properties` has higher 
  precedence than the `-Pkotlin.native.enabled=true` specified in the Gradle command.

## Using -dev versions

We publish `-dev` versions frequently.

For `-dev` versions you can use the [list of available versions](https://redirector.kotlinlang.org/maven/bootstrap/org/jetbrains/kotlin/kotlin-compiler/maven-metadata.xml) and include this maven repository:

```kotlin
maven("https://redirector.kotlinlang.org/maven/bootstrap")
```


## Community and Support
Kotlin has a strong, welcoming, and rapidly growing global community. Whether you're a beginner or an experienced developer, there's a wide range of resources available to help you learn, grow, and get involved.

### Official Resources

- **Kotlin Website** – Central hub for all things Kotlin  
  [https://kotlinlang.org](https://kotlinlang.org)

- **Kotlin Documentation** – In-depth guides, tutorials, and API reference  
  [https://kotlinlang.org/docs/home.html](https://kotlinlang.org/docs/home.html)

- **Kotlin Playground** – Write, run, and share Kotlin code directly in the browser  
  [https://play.kotlinlang.org](https://play.kotlinlang.org)

- **Kotlin Blog** – News, release updates, and insights from the Kotlin team  
  [https://blog.jetbrains.com/kotlin/](https://blog.jetbrains.com/kotlin/)

- **Issue Tracker** – Report bugs or request features via JetBrains' YouTrack  
  [https://youtrack.jetbrains.com/issues/KT](https://youtrack.jetbrains.com/issues/KT)

### Community Channels

- **Kotlin Slack (Public)** – Over 30,000 members sharing knowledge and helping each other  
  [https://slack.kotlinlang.org/](https://slack.kotlinlang.org/)

- **Kotlin Forums** – Ask questions and join discussions with other Kotlin developers  
  [https://discuss.kotlinlang.org](https://discuss.kotlinlang.org)

- **Stack Overflow** – A large Kotlin tag with thousands of answered questions  
  [https://stackoverflow.com/questions/tagged/kotlin](https://stackoverflow.com/questions/tagged/kotlin)

- **YouTube Channel** – Official Kotlin talks, tutorials, and livestreams  
  [https://www.youtube.com/@kotlin](https://www.youtube.com/@kotlin)

- **Twitter (X)** – Follow for news, tips, and community highlights  
  [https://twitter.com/kotlin](https://twitter.com/kotlin)

### Events and Meetups

- **KotlinConf** – The official Kotlin conference hosted by JetBrains  
  [https://kotlinconf.com](https://kotlinconf.com)

- **Community Meetups** – Kotlin user groups and community-run events around the world  
  [https://kotlinlang.org/community/user-groups.html](https://kotlinlang.org/community/user-groups.html)

# License
Kotlin is distributed under the terms of the Apache License (Version 2.0). See [license folder](license/README.md) for details.

# Contributing

Please be sure to review Kotlin's [contributing guidelines](docs/contributing.md) to learn how to help the project.

# Kotlin Foundation

The Kotlin Foundation is a non-profit organization whose mission is to promote and advance the Kotlin ecosystem. You can learn more about the structure and goals of the Kotlin Foundation on its [official website](https://kotlinfoundation.org/).
