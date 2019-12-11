// !LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations, -ProhibitAssigningSingleElementsToVarargsInNamedForm
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    foo(s = 1)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s = i)
    foo(s = *i)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s = intArrayOf(1))
    foo(s = *intArrayOf(1))
    foo(1)

    Cls(p = 1)

    class Sub : Cls(p = 1)

    val c = object : Cls(p = 1) {}

    foo(s = *intArrayOf(elements = 1))
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    anyFoo(a = "")
    anyFoo(a = arrayOf(""))
    anyFoo(a = *arrayOf(""))
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    genFoo<Int>(t = 1)
    genFoo<Int?>(t = null)
    genFoo<Array<Int>>(t = arrayOf())
    genFoo<Array<Int>>(t = *arrayOf(arrayOf()))

    genFoo(t = "")
    genFoo(t = arrayOf(""))
    genFoo(t = *arrayOf(""))
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    manyFoo(v = 1)
    manyFoo(s = "")

    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(a)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(v = a)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(s = a)
    manyFoo(v = a as Int)
    manyFoo(s = a as String)
}