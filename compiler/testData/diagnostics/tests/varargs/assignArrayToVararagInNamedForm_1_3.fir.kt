// !LANGUAGE: -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann(vararg val s: String, val x: Int)

fun withVararg(vararg s: String) {}
fun foo() {}

fun test_fun(s: String, arr: Array<String>) {
    <!INAPPLICABLE_CANDIDATE!>withVararg<!>(arr) // Error
    withVararg(*arr) // OK
    withVararg(s = arr) // Error
    withVararg(s = *arr) // OK

    withVararg(s) // OK
    <!INAPPLICABLE_CANDIDATE!>withVararg<!>(s = s) // Error
}

fun test_ann(s: String, arr: Array<String>) {
    @Ann([""], x = 1)
    foo()
    @Ann(*[""], x = 1)
    foo()
    @Ann(s = [""], x = 1)
    foo()
    @Ann(s = *[""], x = 1)
    foo()

    @Ann("", x = 1)
    foo()
    @Ann(s = "", x = 1)
    foo()
}
