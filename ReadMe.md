[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
<a href="http://slack.kotlinlang.org/"><img src="http://slack.kotlinlang.org/badge.svg" height="20"></a>
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_dev_Compiler.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_dev_Compiler&branch_Kotlin_dev=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Kotlin Programming Language

Welcome to [Kotlin](https://kotlinlang.org/)! Some handy links:

 * [Kotlin Site](https://kotlinlang.org/)
 * [Getting Started Guide](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Try Kotlin](https://try.kotlinlang.org/)
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
 * [Kotlin TextMate Bundle](https://github.com/vkostyukov/kotlin-sublime-package)

## Build environment requirements

In order to build Kotlin distribution you need to have:

- JDK 1.6, 1.7 and 1.8
- Setup environment variables as following:

        JAVA_HOME="path to JDK 1.8"
        JDK_16="path to JDK 1.6"
        JDK_17="path to JDK 1.7"
        JDK_18="path to JDK 1.8"

For local development, if you're not working on bytecode generation or the standard library, it's OK to have only JDK 8 installed, and to point all of the environment variables mentioned above to your JDK 8 installation.

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

These dependencies are quite large, so depending on the qualify of your internet connection 
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

## <a name="working-in-idea"></a> Working with the project in IntelliJ IDEA

Working with the Kotlin project requires IntelliJ IDEA 2017.3. You can download IntelliJ IDEA 2017.3 [here](https://www.jetbrains.com/idea/download).

To import the project in Intellij choose project directory in Open project dialog. Then, after project opened, Select 
`File` -> `New...` -> `Module from Existing Sources` in the menu, and select `build.gradle.kts` file in the project's root folder.

In the import dialog, select `use default gradle wrapper`.

To be able to run tests from IntelliJ easily, check `Delegate IDE build/run actions to Gradle` in the Gradle runner settings.

At this time, you can use the latest released 1.2.x version of the Kotlin plugin for working with the code. To make sure you have the latest version installed, use Tools | Kotlin | Configure Kotlin Plugin Updates and press "Check for updates now".

### Compiling and running

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest and greatest IDEA plugin

* VCS -> Git -> Pull
* Run the "IDEA" run configuration in the project
* a child IntelliJ IDEA with the Kotlin plugin will then startup

# Contributing

We love contributions! There's [lots to do on Kotlin](https://youtrack.jetbrains.com/issues/KT) and on the
[standard library](https://youtrack.jetbrains.com/issues/KT?q=%23Kotlin%20%23Unresolved%20and%20(links:%20KT-2554,%20KT-4089%20or%20%23Libraries)) so why not chat with us
about what you're interested in doing? Please join the #kontributors channel in [our Slack chat](http://slack.kotlinlang.org/)
and let us know about your plans.

If you want to find some issues to start off with, try [this query](https://youtrack.jetbrains.com/issues/KT?q=tag:%20%7BUp%20For%20Grabs%7D%20%23Unresolved) which should find all Kotlin issues that marked as "up-for-grabs".

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [standard library docs](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
and find classes or functions which are not documented very well and submit a patch.

In particular it'd be great if all functions included a nice example of how to use it such as for the
[`hashMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/hash-map-of.html) function.
This is implemented using the [`@sample`](https://github.com/JetBrains/kotlin/blob/1.1.0/libraries/stdlib/src/kotlin/collections/Maps.kt#L91)
macro to include code from a test function. The benefits of this approach are twofold; First, the API's documentation is improved via beneficial examples that help new users and second, the code coverage is increased.

Also the [JavaScript translation](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) could really use your help. See the [JavaScript contribution section](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) for more details.

Some of the code in the standard library is created by generating code from templates. See the [README](libraries/stdlib/ReadMe.md) in the stdlib section for how to run the code generator. The existing templates can be used as examples for creating new ones.

## Submitting patches

The best way to submit a patch is to [fork the project on github](https://help.github.com/articles/fork-a-repo/) then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [github](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.
