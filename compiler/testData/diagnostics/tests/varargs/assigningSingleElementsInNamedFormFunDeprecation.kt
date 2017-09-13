// !LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>)
    foo(s = <!TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>i<!>)
    foo(s = *i)
    foo(s = <!TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>intArrayOf(1)<!>)
    foo(s = *intArrayOf(1))
    foo(1)

    Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>)

    class Sub : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>)

    val c = object : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>) {}

    foo(s = *intArrayOf(elements = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>))
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>""<!>)
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>arrayOf("")<!>)
    anyFoo(a = *arrayOf(""))
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    genFoo<Int>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>)
    genFoo<Int?>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>null<!>)
    genFoo<Array<Int>>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>arrayOf()<!>)
    genFoo<Array<Int>>(t = *arrayOf(arrayOf()))

    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>""<!>)
    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>arrayOf("")<!>)
    genFoo(t = *arrayOf(""))
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>1<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>""<!>)

    <!NONE_APPLICABLE!>manyFoo<!>(a)
    manyFoo(v = <!TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>a<!>)
    manyFoo(s = <!TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>a<!>)
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>a as Int<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM!>a as String<!>)
}