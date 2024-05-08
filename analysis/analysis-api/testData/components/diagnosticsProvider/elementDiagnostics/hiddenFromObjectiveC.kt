// !LANGUAGE: +MultiPlatformProjects

// MODULE: lib
// TARGET_PLATFORM: Common
// FILE: lib.kt
package lib

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MetaAnno

@MetaAnno
annotation class Anno


// MODULE: main(lib)
// TARGET_PLATFORM: Native
// FILE: main.kt
package main

import lib.*

@Anno
abstract class Base

<expr>class Impl : Base()</expr>