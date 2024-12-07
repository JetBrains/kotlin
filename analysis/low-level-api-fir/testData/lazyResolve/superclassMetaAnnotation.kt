// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// MODULE: lib
// FILE: lib.kt
package lib

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MetaAnno

@MetaAnno
annotation class Anno


// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

@Anno
abstract class Base

class Im<caret>pl : Base()