// LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations, +ProhibitAssigningSingleElementsToVarargsInNamedForm -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, TYPE_MISMATCH!>i<!>)
    foo(s = *i)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, TYPE_MISMATCH!>intArrayOf(1)<!>)
    foo(s = *intArrayOf(1))
    foo(1)

    Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>)

    class Sub : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>)

    val c = object : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>) {}

    foo(s = *intArrayOf(elements = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>))
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>)
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf("")<!>)
    anyFoo(a = *arrayOf(""))
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    genFoo<Int>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>)
    genFoo<Int?>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>null<!>)
    genFoo<Array<Int>>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf()<!>)
    genFoo<Array<Int>>(t = *arrayOf(arrayOf()))

    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>)
    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf("")<!>)
    genFoo(t = *arrayOf(""))
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>1<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>)

    <!NONE_APPLICABLE!>manyFoo<!>(a)
    <!NONE_APPLICABLE!>manyFoo<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>v<!> = a)
    <!NONE_APPLICABLE!>manyFoo<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>s<!> = a)
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>a as Int<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>a as String<!>)
}
