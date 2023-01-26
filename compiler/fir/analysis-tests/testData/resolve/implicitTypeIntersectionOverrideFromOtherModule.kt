// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// MODULE: lib
package dependency

abstract class A {
    abstract fun foo()
}
interface I {
    fun foo()
}

// MODULE: main(lib)
// FILE: B.kt
package main
import dependency.A
import dependency.I

abstract class B : A(), I {}

fun usage(b : B?) {
    if (b != null) {
        b.foo()
    }
}