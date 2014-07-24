# Kotlin Programming Language

Welcome to [Kotlin](http://kotlinlang.org/)! Some handy links:

 * [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started)
 * [Web Demo](http://kotlin-demo.jetbrains.com/)
 * [Kotlin Site](http://jetbrains.github.com/kotlin/)
 * [API](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html)
 * [Issue Tracker](http://youtrack.jetbrains.com/issues/KT)
 * [Forum](http://devnet.jetbrains.net/community/kotlin?view=discussions)
 * [Kotlin Blog](http://blog.jetbrains.com/kotlin/)
 * [follow Kotlin on twitter](http://twitter.com/#!/project_kotlin)
 * [TeamCity CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview)

## Editing Kotlin

 * [Kotlin IDEA Plugin](http://hadihariri.com/2012/02/17/the-kotlin-journey-part-i-getting-things-set-up/)
 * [Kotlin TextMate Bundle](https://github.com/k33g/kotlin-textmate-bundle#readme)

## Building

To build this project, first time you try to build you need to run this (requires Apache Ant 1.8 or higher):

    ant -f update_dependencies.xml

which will setup the dependencies on

* intellij-core: is a part of command line compiler and contains only necessary APIs.
* idea-full: is a full blown IntelliJ IDEA Community Edition to be used in former plugin module.

Then, you need to run

    ant -f build.xml
    
which will build the binaries of the compiler and put them into the 'dist' directory. You may need to increase the **heap size** for Ant using
[ANT_OPTS](http://www.liferay.com/community/wiki/-/wiki/Main/Ant+opts).

**OPTIONAL:** Maven distribution is built separately, run

    mvn package

from 'libraries' directory after building the compiler. Refer to `libraries/ReadMe.md` for details.

## Working with the project in IDEA

The [root kotlin project](https://github.com/JetBrains/kotlin) already has an IDEA project, you can just open it in IDEA.

You may need to set the Project SDK (File -> Project Structure -> Project).
You may also need to add `tools.jar` to your SDK: File -> Project Structure -> SDKs -> <Your JDK> -> Classpath,
then choose the `tools.jar` in the JDK's `lib` directory.

If you are not dealing with Android, you may need to disable the Android Plugin in order to compile the project.

Since Kotlin project contains code written in Kotlin itself, you will also need a Kotlin plugin to build the project in IntelliJ IDEA.
To keep the plugin version in sync with the rest of the team and our [Continuous Integration server](http://teamcity.jetbrains.com/project.html?projectId=Kotlin&tab=projectOverview)
you should install the according to the [instructions below](#plugin-for-contributors).

If you want to have an IDEA installation without the Kotlin plugin which is separate to your default IDEA installation which has the Kotlin
plugin [see this document](http://devnet.jetbrains.net/docs/DOC-181) which describes how to have mutliple IDEA installs using different configurations and plugin directories.

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest greatest IDEA plugin

* VCS -> Git -> Pull
* Run IDEA
* a child IDEA with the Kotlin plugin will then startup
* you can now open the [kotlin libraries project](https://github.com/JetBrains/kotlin/tree/master/libraries) to then work with the various kotlin libraries etc.

### <a name="pre-built-plugin"></a>Using a pre-built Kotlin IDEA plugin

There are several options for getting Kotlin plugin. A stable version can be obtained as any other plugin for Intellij IDEA:

    Preferences -> Plugins -> Browse Repositories -> Search with "Kotlin" string

The most recent version of the plugin can be downloaded from the
[IDEA Plugin and Tests CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview). When downloading is
finished you can install it with "Install plugin from disk...":

    Preferences -> Plugins -> Install plugin from disk...

You can now open any Kotlin based projects.

<a name="plugin-for-contributors"></a>
**Note for contributors**: If you are planning to contribute to Kotlin project you probably want to have locally the same version of plugin that build server is using for building.
As this version is constantly moving, the best way to always be updated is to let IDEA notify you when it is time to renew you plugin.

Open 

    Preferences -> Plugins -> Browse Repositories -> Manage Repositories...

and add the following URL to your repositories:

    http://teamcity.jetbrains.com/guestAuth/repository/download/bt345/bootstrap.tcbuildtag/updatePlugins.xml

Then update the list of plugins in "Browse Repositories", you'll see two versions of Kotlin there, install the one with the higher version number.

# Contributing

We love contributions! There's [lots to do on kotlin](http://youtrack.jetbrains.com/issues/KT) and on the [standard library](http://youtrack.jetbrains.com/issues/KT?q=%23%7BStandard+Library%7D+-Resolved) so why not chat with us on the [forum](http://devnet.jetbrains.net/community/kotlin?view=discussions) about what you're interested in doing?

If you want to find some issues to start off with, try [this query](http://youtrack.jetbrains.com/issues?q=tag%3A+%7BUp+For+Grabs%7D) which should find all issues that marked as "up-for-grabs".

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [API docs](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html) and find classes or functions which are not documented very well and submit a patch.

In particular it'd be great if all functions included a nice example of how to use it such as for the <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#filter(kotlin.Function1)">filter()</a> function on Collection. This is implemented using the <a href="https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/src/kotlin/IterablesLazy.kt#L17">@includeFunctionBody</a> macro to include code from a test function. This serves as a double win; the API gets better documented with nice examples to help new users and the code gets more test coverage.

Also the [JavaScript translation](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) could really use your help. See the [JavaScript contribution section](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) for more details.


## If you want to work on the compiler

The Kotlin compiler is written in Java and Kotlin (we gradually migrate more and more of it to pure Kotlin). So the easiest way to work on the compiler or IDEA plugin is

* download a clean [IDEA 14 EAP build](http://confluence.jetbrains.com/display/IDEADEV/IDEA+14+EAP)
* [install the Kotlin plugin](#pre-built-plugin)
* open the [root kotlin project](https://github.com/JetBrains/kotlin) in IDEA (opening the kotlin directory)

You can now run the various Run/Debug Configurations such as

* IDEA
* All Compiler Tests
* All IDEA Plugin Tests


## If you want to work on the Kotlin libraries

* download a clean [IDEA 14 EAP build](http://confluence.jetbrains.com/display/IDEADEV/IDEA+14+EAP)
* [install the Kotlin plugin](#pre-built-plugin)
* open the [kotlin libraries project](https://github.com/JetBrains/kotlin/tree/master/libraries)

Then build via

    cd libraries
    mvn install

Some of the code in the standard library is created by generating code from templates. See the [README](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/ReadMe.md) in the stdlib section for how run the code generator. The existing templates can be used as examples for creating new ones.

## Submitting patches

The best way to submit a patch is to [fork the project on github](http://help.github.com/fork-a-repo/) then send us a
[pull request](http://help.github.com/send-pull-requests/) via [github](http://github.com).

If you create your own fork, it might help to [enable rebase by default when you pull](http://d.strelau.net/post/47338904/git-pull-rebase-by-default)
which will avoid your local repo having too many merge commits which will help keep your pull request simple and easy to apply.

## Commit comments

If you include in your comment this text (where KT-1234 is the Issue ID in the [Issue Tracker](http://youtrack.jetbrains.com/issues/KT), the issue will get automatically marked as fixed.

    #KT-1234 Fixed
