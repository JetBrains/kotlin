// ALLOW_AST_ACCESS
package util

interface T {
    fun f()
}

interface T2 {
    fun g() {
    }
}

@Suppress"UNRESOLVED_REFERENCE")
class Invalid: I

class A(val i: Int)

@Suppress"FINAL_SUPERTYPE")
class B: A(1) {
}

open class C(val s: String, i: Int): A(i), T {
    override fun f() {}
}

fun topLevelFun(u: A): A {
    return A(u.i)
}

val topLevelVal: A = C("", 0)
var topLevelVar: A = B()

object topLevelObject: T {
    override fun f() {

    }

    fun g() {
    }
}

fun funWithUnspecifiedType() = 3

fun funWithVararg(vararg i: Int): IntArray {
    return i
}

fun <T, G> funWithWhere(a: G, b: T) where T: Collection<G> {

}

annotation class TempAnnotation
fun funWithAnnotatedParameter(@TempAnnotation param: String) {

}
