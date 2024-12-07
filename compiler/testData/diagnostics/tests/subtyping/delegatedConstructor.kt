// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// MODULE: lib

// FILE: lib.kt
package lib

abstract class Base<in T>


// MODULE: test(lib)

// FILE: foo.kt
package test

import lib.Base

class Foo {
    private inner class FooImpl : Base<Any?>()
}

// FILE: bar.kt
package test

import lib.Base

class Bar {
    private inner class BarImpl : Base<Any?>()
}