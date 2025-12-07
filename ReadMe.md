<div align="center">

[![official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_KotlinPublic_Compiler.svg)](https://teamcity.jetbrains.com/buildConfiguration/Kotlin_KotlinPublic_Compiler?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.jetbrains.com/scans?search.rootProjectNames=Kotlin)

<img src="https://upload.wikimedia.org/wikipedia/commons/7/74/Kotlin_Icon.png" width="120" alt="Kotlin logo" />

# Kotlin Programming Language

_Kotlin: Concise • Safe • Multiplatform_

Kotlin is a concise multiplatform language developed by [JetBrains](https://www.jetbrains.com/) and
[contributors](https://kotlinlang.org/docs/contribute.html).

---

### 🔗 Helpful Resources

[Website](https://kotlinlang.org/) •
[Getting Started](https://kotlinlang.org/docs/tutorials/getting-started.html) •
[Try Kotlin](https://play.kotlinlang.org/) •
[Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/index.html) •
[Issue Tracker](https://youtrack.jetbrains.com/issues/KT) •
[YouTube Channel](https://www.youtube.com/channel/UCP7uiEZIqci43m22KDl0sNw) •
[Blog](https://blog.jetbrains.com/kotlin/) •
[Forum](https://discuss.kotlinlang.org/) •
[Slack Community](https://slack.kotlinlang.org/) •
[Twitter](https://twitter.com/kotlin) •
[TeamCity CI](https://teamcity.jetbrains.com/project.html?tab=projectOverview&projectId=Kotlin) •
[Kotlin Foundation](https://kotlinfoundation.org/)

</div>

---

## 📚 Table of Contents

- [Kotlin Multiplatform capabilities](#kotlin-multiplatform-capabilities)
- [Editing Kotlin](#editing-kotlin)
- [Build environment requirements](#build-environment-requirements)
- [Building](#building)
    - [Important Gradle tasks](#important-gradle-tasks)
- [Working with the project in IntelliJ IDEA](#working-with-the-project-in-intellij-idea)
    - [Dependency verification](#dependency-verification)
- [Using -dev versions](#using--dev-versions)
- [License](#license)
- [Contributing](#contributing)
- [Kotlin Foundation](#kotlin-foundation)

---

## 💻📱🖥 Kotlin Multiplatform Capabilities

Kotlin enables sharing code across [different platforms](https://kotlinlang.org/docs/reference/mpp-supported-platforms.html)
— while offering native performance and flexibility.

Use:

- [Kotlin Multiplatform](https://www.jetbrains.com/kotlin-multiplatform/)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)

To share:

- Business logic
- UI components
- Across Android, iOS, desktop and web

More details:

- [Get Started](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Benefits](https://kotlinlang.org/docs/reference/multiplatform.html)
- [Share code on all platforms](https://kotlinlang.org/docs/reference/mpp-share-on-platforms.html#share-code-on-all-platforms)
- [Share code on similar platforms](https://kotlinlang.org/docs/reference/mpp-share-on-platforms.html#share-code-on-similar-platforms)

---

## 🛠️ Editing Kotlin

Choose your favorite editor or IDE:

- [Kotlin IntelliJ Plugin](https://kotlinlang.org/docs/tutorials/getting-started.html)  
  ([source](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin))
- [Kotlin Eclipse Plugin](https://kotlinlang.org/docs/tutorials/getting-started-eclipse.html)
- [Kotlin Sublime Text Package](https://github.com/vkostyukov/kotlin-sublime-package)

---
## ⚙️ Build Environment Requirements

This repository is using [Gradle toolchains](https://docs.gradle.org/current/userguide/toolchains.html) feature
to select and auto-provision required JDKs from [AdoptOpenJdk](https://adoptopenjdk.net) project.

Alternatively, it is still possible to only provide required JDKs via environment variables 
(see [gradle.properties](./gradle.properties#L5) for supported variable names). To ensure Gradle uses only JDKs 
from environmental variables - disable Gradle toolchain auto-detection by passing `-Porg.gradle.java.installations.auto-detect=false` option
(or put it into `$GRADLE_USER_HOME/gradle.properties`).

On Windows you might need to add long paths setting to the repo:

    git config core.longpaths true 
---
## 🔨 Building

The project is built with Gradle. Run Gradle to build the project and to run the tests 
using the following command on Unix/macOS:

    ./gradlew <tasks-and-options>
    
or the following command on Windows:

    gradlew <tasks-and-options>

On the first project configuration gradle will download and setup the dependencies on:

* `intellij-core` is a part of command line compiler and contains only necessary APIs.
* `idea-full` is a full blown IntelliJ IDEA Community Edition to be used in the plugin module.

These dependencies are quite large, so depending on the quality of your internet connection 
you might face timeouts getting them. In this case, you can increase timeout by specifying the following 
command line parameters on the first run: 
    
    ./gradlew -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000
---
## 📌 Important gradle tasks

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
---
## <a name="working-in-idea"></a> 🧑‍💻 Working with the project in IntelliJ IDEA

It is recommended to use the latest released version of Intellij IDEA (Community or Ultimate Edition). You can download IntelliJ IDEA [here](https://www.jetbrains.com/idea/download).

After cloning the project, import the project in IntelliJ by choosing the project directory in the Open project dialog.

For handy work with compiler tests it's recommended to use [Kotlin Compiler Test Helper](https://github.com/demiurg906/test-data-helper-plugin).

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

---
## ⚡ Using -dev versions

We publish `-dev` versions frequently.

For `-dev` versions you can use the [list of available versions](https://redirector.kotlinlang.org/maven/bootstrap/org/jetbrains/kotlin/kotlin-compiler/maven-metadata.xml) and include this maven repository:

```kotlin
maven("https://redirector.kotlinlang.org/maven/bootstrap")
```

---

## 📜 License

Kotlin is distributed under the terms of the **Apache License 2.0**.

You can find full licensing details here:  
➡️ [`/license`](license/README.md)

---

## 🤝 Contributing

We love community contributions! 💜  
Before you begin, please review our guidelines:

➡️ [`docs/contributing.md`](docs/contributing.md)

This will help you understand development workflow, code style, and how to submit changes successfully.

---

## 🏛 Kotlin Foundation

The **Kotlin Foundation** is a non-profit organization dedicated to promoting and advancing the Kotlin ecosystem,
ensuring long-term development and community support.

Learn more:  
➡️ https://kotlinfoundation.org/

---

<div align="center">

Made with ❤️ by JetBrains & the Kotlin Community

</div>
