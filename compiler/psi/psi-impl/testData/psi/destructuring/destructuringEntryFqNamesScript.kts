// COMPILATION_ERRORS
// KT-90376
package test.fqNames

data class X(val a: Int, val b: Int)

val (scriptA, scriptB) = X(1, 2)

class C {
    val (memberA, memberB) = X(1, 2)
}

fun foo() {
    val (localA, localB) = X(1, 2)
}
