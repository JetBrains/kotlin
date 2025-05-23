// RUN_PIPELINE_TILL: BACKEND
// MODULE: dependency
// FILE: dependency.kt
package org.example

interface Base

abstract class Foo : Base

// MODULE: usage(dependency)
// FILE: usage.kt
package org.example

interface Base {
    fun bar()
    fun baz()
}

fun test() {
    abstract class FooImpl : Foo() {
        fun usage() {
            bar()
        }

        override fun baz() {

        }
    }
}
