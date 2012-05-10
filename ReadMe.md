# Kotlin Programming Language

Welcome to [Kotlin](http://www.jetbrains.com/kotlin)! Some handy links:

 * [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started)
 * [Web Demo](http://kotlin-demo.jetbrains.com/)
 * [Kotlin Site](http://jetbrains.github.com/kotlin/)
 * [API](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html)
 * [Issue Tracker](http://youtrack.jetbrains.com/issues/KT)
 * [Kotlin Blog](http://blog.jetbrains.com/kotlin/)
 * [follow Kotlin on twitter](http://twitter.com/#!/project_kotlin)

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
    
which will build the binaries of the comppiler and put them into the 'dist' directory.

## Working with the project in IDEA

The [root kotlin project](https://github.com/JetBrains/kotlin) already has an IDEA project, you can just open it in IDEA.

**Note** though that you need a recent IDEA build (e.g. [11 EAP](http://confluence.jetbrains.net/display/IDEADEV/IDEA+11.1+EAP)) which should **not** contain the Kotlin plugin!

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest greatest IDEA plugin

* VCS -> Git -> Pull
* Run IDEA
* a child IDEA with the Kotlin plugin will then startup
* you can now open the [kotlin libraries project](https://github.com/JetBrains/kotlin/libraries) to then work with the various kotlin libraries etc.

### Using a pre-built Kotlin IDEA plugin

You can download the latest Kotlin IDEA Plugin from the [IDEA Plugin and Tests CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview)

Or in a recent [IDEA 11 EAP build](http://confluence.jetbrains.net/display/IDEADEV/IDEA+11.1+EAP) install the Kotlin plugin:

Preferences -> Plugins -> Browse Repositories -> Manage Repositories... -> + to add a new repository URL

 * [http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml](http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml)

You can now open any Kotlin based projects. Its advisable you don't open the [root kotlin project](https://github.com/JetBrains/kotlin) as thats intended to be used to
build the kotlin compiler and plugin itself; instead open the [kotlin libraries project](https://github.com/JetBrains/kotlin/libraries)


## If you want to work on the compiler

The Kotlin compiler is currently all written in Java (we plan to port it to Kotlin later). So the easiest way to work on the compiler or IDEA plugin is

* download a clean [IDEA 11 EAP build](http://confluence.jetbrains.net/display/IDEADEV/IDEA+11.1+EAP)
* don't install the Kotlin plugin
* open the [root kotlin project](https://github.com/JetBrains/kotlin) in IDEA (opening the kotlin directory)

You can now run the various Run/Debug Configurations such as

* IDEA
* All Compiler Tests
* All IDEA Plugin Tests


## If you want to work on the Kotlin libraries

* download a clean [IDEA 11 EAP build](http://confluence.jetbrains.net/display/IDEADEV/IDEA+11.1+EAP)
* Preferences -> Plugins -> Browse Repositories -> Manage Repositories... -> + to add a new repository URL
* [http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml](http://www.jetbrains.com/kotlin/eap-plugin-repository/updatePlugins.xml)
* open the [kotlin libraries project](https://github.com/JetBrains/kotlin/libraries)

Then build via

    cd libraries
    mvn install


## Contributing

We love contributions! There's [lots to do on kotlin](http://youtrack.jetbrains.com/issues/KT) and on the [standard library](http://youtrack.jetbrains.com/issues/KT?q=%23%7BStandard+Library%7D+-Resolved) so why not chat with us on the [forum](http://devnet.jetbrains.net/community/kotlin?view=discussions) about what you're interested in doing?

If you want to find some issues to start off with, try [this query](http://youtrack.jetbrains.com/issues/KT?q=-Resolved+%23Kontributor) which should find all issues that are not resolved assigned to Kontributor.
Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [API docs](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html) and find classes or functions which are not documented very well and submit a patch.

In particular it'd be great if all functions included a nice example of how to use it such as for the <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#filter(jet.Function1)">filter()</a> function on Collection. This is implemented using the <a href="https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/src/kotlin/JLangIterablesLazy.kt#L17">@includeFunctionBody</a> macro to include code from a test function. This serves as a double win; the API gets better documented with nice examples to help new users and the code gets more test coverage.

## Submitting patchces

The best way to submit a patch is to [fork the project on github](http://help.github.com/fork-a-repo/) then send us a
[pull request](http://help.github.com/send-pull-requests/) via [github](http://github.com).

If you create your own fork, it might help to [enable rebase by default when you pull](http://d.strelau.net/post/47338904/git-pull-rebase-by-default)
which will avoid your local repo having too many merge commits which will help keep your pull request simple and easy to apply.

## Commit comments

If you include in your comment this text (where KT-1234 is the Issue ID in the [Issue Tracker](http://youtrack.jetbrains.com/issues/KT), the issue will get automatically marked as fixed.

    #KT-1234 Fixed


## Kommitter links

* [TeamCity CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview)