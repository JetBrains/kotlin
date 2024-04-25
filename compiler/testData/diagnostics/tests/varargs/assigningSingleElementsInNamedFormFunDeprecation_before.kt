// LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations, -ProhibitAssigningSingleElementsToVarargsInNamedForm -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING, TYPE_MISMATCH!>i<!>)
    foo(s = *i)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING, TYPE_MISMATCH!>intArrayOf(1)<!>)
    foo(s = *intArrayOf(1))
    foo(1)

    Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>)

    class Sub : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>)

    val c = object : Cls(p = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>) {}

    foo(s = *intArrayOf(elements = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>))
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>""<!>)
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>arrayOf("")<!>)
    anyFoo(a = *arrayOf(""))
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    genFoo<Int>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>)
    genFoo<Int?>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>null<!>)
    genFoo<Array<Int>>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>arrayOf()<!>)
    genFoo<Array<Int>>(t = *arrayOf(arrayOf()))

    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>""<!>)
    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>arrayOf("")<!>)
    genFoo(t = *arrayOf(""))
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>1<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>""<!>)

    <!NONE_APPLICABLE!>manyFoo<!>(a)
    <!NONE_APPLICABLE!>manyFoo<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>v<!> = a)
    <!NONE_APPLICABLE!>manyFoo<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>s<!> = a)
    manyFoo(v = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>a as Int<!>)
    manyFoo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_WARNING!>a as String<!>)
}
