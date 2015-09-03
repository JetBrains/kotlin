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
        val v = oldFun2(i as Int) as Int
        /* comment */
        continue@loop
    }

    oldFun1(oldFun2(10))

    oldFun2()
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
