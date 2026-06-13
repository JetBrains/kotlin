// MODULE: lib
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
@file:MyFileAnnotationSource("source")
@file:MyFileAnnotationBinary("binary")
@file:MyFileAnnotationRuntime("runtime")

package lib

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class MyFileAnnotationSource(val value: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyFileAnnotationBinary(val value: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyFileAnnotationRuntime(val value: String)

class A {
    fun method() {}
}

typealias B = A

fun foo(x: Int) = 42

val bar: Int
    get() = 42

// MODULE: main(lib)
// FILE: main.kt
@file:MyFileAnnotationSource("source")
@file:MyFileAnnotationBinary("binary")
@file:MyFileAnnotationRuntime("runtime")

import lib.*

fun test() {
    val a: A? = null
    val b: B? = null
    foo(0)
    bar
    A().method()
}
