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

To build this project you need to run

    ant -f update_dependencies.xml

which will setup the dependencies on

* intellij-core: is a part of command line compiler and contains only necessary APIs.
* idea-full: is a full blown IntelliJ IDEA Community Edition to be used in former plugin module.

Then, you need to run

    ant -f build.xml
    
which will build the binaries and put them into the 'dist' directory.