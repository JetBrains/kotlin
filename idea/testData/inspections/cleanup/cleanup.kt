import pack.oldFun1
import pack.oldFun2 // should not be removed for non-deprecated overload used
import pack.oldFun3

trait Foo {
}

val f = { (a: Int, b: Int) -> a + b }

class A private()

val x = fun foo(x: String) { }

fun foo() {
    @loop
    for (i in 1..100) {
        val v = oldFun2(i as Int)/* as Int*/
        /* comment */
        continue@loop
    }

    oldFun1(oldFun2(10))

    oldFun2()

    ff()
    val xx = valX
    varX = 50
    varY = 60
}

fun unnecessarySafeCall(x: String) {
    x?.length()
}

fun unnecessaryExclExcl(x: String) {
    x!!.length()
}

fun unnecessaryCast(x: String) = x as String

fun unnecessaryElvis(x: String) = x ?: ""

JavaAnn(1, "abc") class MyClass

annotation class Ann()

Ann class A1

Ann() class A2

kotlin.data class A3

@inline @private fun <T> baz() {
    @suppress("UNCHECKED_CAST")
    (1 as T)

    @data class Local
}

deprecated("123", ReplaceWith("34")) class Obsolete

native fun nativeFun(): Int
