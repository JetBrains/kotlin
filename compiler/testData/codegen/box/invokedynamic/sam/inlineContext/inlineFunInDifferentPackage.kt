// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FILE: inlineFunInDifferentPackage.kt
import a.*

fun box() = test { k -> "O" + k }

// FILE: a.kt
package a

fun interface IFoo {
    fun foo(k: String): String
}

fun fooK(iFoo: IFoo) = iFoo.foo("K")

inline fun test(crossinline lambda: (String) -> String) =
    fooK { k -> lambda(k) }