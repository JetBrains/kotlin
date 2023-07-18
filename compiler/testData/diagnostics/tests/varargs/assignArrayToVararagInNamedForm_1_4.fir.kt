// !LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann(vararg val s: String, val x: Int)

fun withVararg(vararg s: String) {}
fun foo() {}

fun test_fun(s: String, arr: Array<String>) {
    withVararg(<!ARGUMENT_TYPE_MISMATCH!>arr<!>) // Error
    withVararg(*arr) // OK
    withVararg(s = arr) // OK
    withVararg(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arr<!>) // Warning

    withVararg(s) // OK
    withVararg(s = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>s<!>) // Error
}

fun test_ann(s: String, arr: Array<String>) {
    @Ann(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>, x = 1)
    foo()
    @Ann(*[""], x = 1)
    foo()
    @Ann(s = [""], x = 1)
    foo()
    @Ann(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>[""]<!>, x = 1)
    foo()

    @Ann("", x = 1)
    foo()
    @Ann(s = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR!>""<!>, x = 1)
    foo()
}
