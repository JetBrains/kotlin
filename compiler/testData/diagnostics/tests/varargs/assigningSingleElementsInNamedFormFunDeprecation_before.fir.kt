// !LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations, -ProhibitAssigningSingleElementsToVarargsInNamedForm -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s = 1)
    foo(s = i)
    foo(s = *i)
    foo(s = intArrayOf(1))
    foo(s = *intArrayOf(1))
    foo(1)

    <!INAPPLICABLE_CANDIDATE!>Cls<!>(p = 1)

    class Sub : <!INAPPLICABLE_CANDIDATE!>Cls<!>(p = 1)

    val c = object : <!INAPPLICABLE_CANDIDATE!>Cls<!>(p = 1) {}

    foo(s = *<!INAPPLICABLE_CANDIDATE!>intArrayOf<!>(elements = 1))
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    <!INAPPLICABLE_CANDIDATE!>anyFoo<!>(a = "")
    anyFoo(a = arrayOf(""))
    anyFoo(a = *arrayOf(""))
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    <!INAPPLICABLE_CANDIDATE!>genFoo<!><Int>(t = 1)
    <!INAPPLICABLE_CANDIDATE!>genFoo<!><Int?>(t = null)
    genFoo<Array<Int>>(t = arrayOf())
    genFoo<Array<Int>>(t = *arrayOf(arrayOf()))

    <!INAPPLICABLE_CANDIDATE!>genFoo<!>(t = "")
    genFoo(t = arrayOf(""))
    genFoo(t = *arrayOf(""))
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(v = 1)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(s = "")

    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(a)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(v = a)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(s = a)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(v = a as Int)
    <!INAPPLICABLE_CANDIDATE!>manyFoo<!>(s = a as String)
}