// !LANGUAGE: -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann(vararg val s: String, val x: Int)

fun withVararg(vararg s: String) {}
fun foo() {}

fun test_fun(s: String, arr: Array<String>) {
    withVararg(<!TYPE_MISMATCH!>arr<!>) // Error
    withVararg(*arr) // OK
    withVararg(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, TYPE_MISMATCH!>arr<!>) // Error
    withVararg(s = *arr) // OK

    withVararg(s) // OK
    withVararg(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>s<!>) // Error
}

fun test_ann(s: String, arr: Array<String>) {
    @Ann(<!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, TYPE_MISMATCH!>[""]<!>, x = 1)
    foo()
    @Ann(*[""], x = 1)
    foo()
    @Ann(s = [""], x = 1)
    foo()
    @Ann(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>[""]<!>, x = 1)
    foo()

    @Ann("", x = 1)
    foo()
    @Ann(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR, NI;TYPE_MISMATCH!>""<!>, x = 1)
    foo()
}