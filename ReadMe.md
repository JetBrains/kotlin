# Google Internal Kotlin Mirror

This is the Google internal mirror of the kotlin github repo. The purpose of
this mirror is to allow reviews of changes internally before uploading them as
pull requests at the official JetBrains github repo.

## Cloning and adding commit hooks for code reviews

```
git clone sso://kotlin-internal/kotlin
cd kotlin
curl -Lo `git rev-parse --git-dir`/hooks/commit-msg https://gerrit-review.googlesource.com/tools/hooks/commit-msg ; chmod +x `git rev-parse --git-dir`/hooks/commit-msg
```

## Preparing change and uploading for code review

Create a branch containing your changes. Commit changes locally, and make sure
that there is only one commit on your branch (squash your branch if you have
to). The commit hooks will add a `Change-Id` line in your commit message which
will identify this change for code review. When addressing comments, make sure
to commit them using `git commit -a --amend` to make sure to keep the same
`Change-Id` so that the code review tool will not lose the association.

Code reviews are uploaded with a push:

```
git push origin HEAD:refs/for/master
```

To add a reviewer from the command-line use:

```
git push origin HEAD:refs/for/master%r=ager@google.com
```

This will upload the review and print a URL for the codereview tool.

## Adding JetBrains github as a remote

Add the official JetBrains github repo as a remote called `upstream`:

```
git remote add upstream https://github.com/JetBrains/kotlin.git
```

## Updating the mirror with the latest version from upstream

```
git fetch upstream
git checkout master
git pull upstream master
git push origin master
```

Updating the mirror sometimes contain a lot of files. If gerrit rejects
the push just push directly.

```
git push sso://kotlin-internal.googlesource.com/_direct/kotlin master
```

---

[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_dev_Compiler.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_dev_Compiler&branch_Kotlin_dev=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Kotlin Programming Language

Welcome to [Kotlin](https://kotlinlang.org/)! Some handy links:

 * [Kotlin Site](https://kotlinlang.org/)
 * [Getting Started Guide](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Try Kotlin](https://play.kotlinlang.org/)
 * [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
 * [Issue Tracker](https://youtrack.jetbrains.com/issues/KT)
 * [Forum](https://discuss.kotlinlang.org/)
 * [Kotlin Blog](https://blog.jetbrains.com/kotlin/)
 * [Follow Kotlin on Twitter](https://twitter.com/kotlin)
 * [Public Slack channel](http://slack.kotlinlang.org/)
 * [TeamCity CI build](https://teamcity.jetbrains.com/project.html?tab=projectOverview&projectId=Kotlin)

## Editing Kotlin

 * [Kotlin IntelliJ IDEA Plugin](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Kotlin Eclipse Plugin](https://kotlinlang.org/docs/tutorials/getting-started-eclipse.html)
 * [Kotlin Sublime Text Package](https://github.com/vkostyukov/kotlin-sublime-package)

## Build environment requirements

In order to build Kotlin distribution you need to have:

- JDK 1.6, 1.7, 1.8 and 9
- Setup environment variables as following:

        JAVA_HOME="path to JDK 1.8"
        JDK_16="path to JDK 1.6"
        JDK_17="path to JDK 1.7"
        JDK_18="path to JDK 1.8"
        JDK_9="path to JDK 9"

For local development, if you're not working on bytecode generation or the standard library, it's OK to have only JDK 1.8 and JDK 9 installed, and to point JDK_16 and JDK_17 environment variables to your JDK 1.8 installation.

You also can use [Gradle properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_properties_and_system_properties) to setup JDK_* variables.

> Note: The JDK 6 for MacOS is not available on Oracle's site. You can [download it here](https://support.apple.com/kb/DL1572). 

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
you might face timeouts getting them. In this case you can increase timeout by specifying the following 
command line parameters on the first run: 
    
    ./gradlew -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000

## Important gradle tasks

- `clean` - clean build results
- `dist` - assembles the compiler distribution into `dist/kotlinc/` folder
- `ideaPlugin` - assembles the Kotlin IDEA plugin distribution into `dist/artifacts/Kotlin` folder
- `install` - build and install all public artifacts into local maven repository
- `runIde` - build IDEA plugin and run IDEA with it
- `coreLibsTest` - build and run stdlib, reflect and kotlin-test tests
- `gradlePluginTest` - build and run gradle plugin tests
- `compilerTest` - build and run all compiler tests
- `ideaPluginTest` - build and run all IDEA plugin tests

**OPTIONAL:** Some artifacts, mainly Maven plugin ones, are built separately with Maven.
Refer to [libraries/ReadMe.md](libraries/ReadMe.md) for details.


### Building for different versions of IntelliJ IDEA and Android Studio

Kotlin plugin is indented to work with several recent versions of IntelliJ IDEA and Android Studio. Each platform is allowed to have a different set of features and might provide a slightly different API. Instead of using several parallel Git branches, project stores everything in a single branch, but files may have counterparts with version extensions (\*.as32, \*.172, \*.181). The primary file is expected to be replaced with its counterpart when targeting non-default platform.

More detailed description of this scheme can be found at https://github.com/JetBrains/bunches/blob/master/ReadMe.md.

Usually, there's no need to care about multiple platforms as all features are enabled everywhere by default. Additional counterparts should be created if there's an expected difference in behavior or an incompatible API usage is required **and** there's no reasonable workaround to save source compatibility. Kotlin plugin contains a pre-commit check that shows a warning if a file has been updated without its counterparts.

Development for some particular platform is possible after 'switching' that can be done with [Bunch Tool](https://github.com/JetBrains/bunches/releases) from the command line.

```sh
cd kotlin-project-dir

# switching to IntelliJ Idea 2018.2
bunch switch . 182
```

## <a name="working-in-idea"></a> Working with the project in IntelliJ IDEA

Working with the Kotlin project requires at least IntelliJ IDEA 2017.3. You can download IntelliJ IDEA 2017.3 [here](https://www.jetbrains.com/idea/download).

After cloning the project, to import the project in Intellij choose the project directory in the Open project dialog. Then, after project opened, Select 
`File` -> `New...` -> `Module from Existing Sources` in the menu, and select `build.gradle.kts` file in the project's root folder.

In the import dialog, select `use default gradle wrapper`.

To be able to run tests from IntelliJ easily, check `Delegate IDE build/run actions to Gradle` and choose `Gradle Test Runner` in the Gradle runner settings after importing the project.

At this time, you can use the latest released 1.2.x version of the Kotlin plugin for working with the code. To make sure you have the latest version installed, use Tools | Kotlin | Configure Kotlin Plugin Updates and press "Check for updates now".

### Compiling and running

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest and greatest IDEA plugin

* VCS -> Git -> Pull
* Run the "IDEA" run configuration in the project
* a child IntelliJ IDEA with the Kotlin plugin will then startup

### Including into composite build

To include kotlin compiler into [composite build](https://docs.gradle.org/current/userguide/composite_builds.html) you need to define `dependencySubstitution` for `kotlin-compiler` module in `settings.gradle`

```
includeBuild('/path/to/kotlin') {
    dependencySubstitution {
        substitute module('org.jetbrains.kotlin:kotlin-compiler') with project(':include:kotlin-compiler')
    }
}
```

# Contributing

Please be sure to review Kotlin's [contributing guidelines](docs/contributing.md) to learn how to help the project.
