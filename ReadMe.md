# Kotlin Programming Language

Welcome to [Kotlin](http://www.jetbrains.com/kotlin)! Some handy links:

 * [Issue Tracker](http://youtrack.jetbrains.com/issues/KT)
 * [Web Demo](http://kotlin-demo.jetbrains.com/)
 * [Kotlin Blog](http://blog.jetbrains.com/kotlin/)
 * [follow Kotlin on twitter](http://twitter.com/#!/project_kotlin)

## Editing Kotlin

 * [Kotlin IDEA Plugin](http://hadihariri.com/2012/02/17/the-kotlin-journey-part-i-getting-things-set-up/)
 * [Kotlin TextMate Bundle](https://github.com/k33g/kotlin-textmate-bundle#readme)

## Building

To build this project, first time you try to build you need to run this:

    ant -f update_dependencies.xml

which will setup the dependencies on

* intellij-core: is a part of command line compiler and contains only necessary APIs.
* idea-full: is a full blown IntelliJ IDEA Community Edition to be used in former plugin module.

Then, you need to run

    ant -f build.xml
    
which will build the binaries of the comppiler and put them into the 'dist' directory.

## Working with the project in IDEA

The [root kotlin project](https://github.com/JetBrains/kotlin) already has an IDEA project, you can just open it in IDEA.

**Note** though that you need a recent IDEA build (e.g. 11 EAP) which should **not** contain the Kotlin plugin!

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest greatest IDEA plugin

* VCS -> Git -> Pull
* Run IDEA
* a child IDEA with the Kotlin plugin will then startup
* you can now open the [kotlin libraries project](https://github.com/JetBrains/kotlin/libraries) to then work with the various kotlin libraries etc.

### Using a pre-built Kotlin IDEA plugin

In a recent IDEA EAP build install the Kotlin plugin:

Preferences -> Plugins -> Browse Repositories -> Manage Repositories... -> + to add a new repository URL

 * [http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml](http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml)

You can now open any Kotlin based projects. Its advisable you don't open the [root kotlin project](https://github.com/JetBrains/kotlin) as thats intended to be used to
build the kotlin compiler and plugin itself; instead open the [kotlin libraries project](https://github.com/JetBrains/kotlin/libraries)


## Kommitter links

* [TeamCity CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview)