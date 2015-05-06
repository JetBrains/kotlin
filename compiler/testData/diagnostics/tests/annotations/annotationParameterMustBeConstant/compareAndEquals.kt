// !DIAGNOSTICS: -UNUSED_VARIABLE
annotation class Ann(vararg val i: Boolean)
fun foo() {
    val a1 = 1 > 2
    val a2 = 1 == 2
    val a3 = a1 == a2
    val a4 = a1 > a2

    @Ann(
            <!ANNOTATION_PARAMETER_MUST_BE_CONST!>a1<!>,
            <!ANNOTATION_PARAMETER_MUST_BE_CONST!>a2<!>,
            <!ANNOTATION_PARAMETER_MUST_BE_CONST!>a3<!>,
            <!ANNOTATION_PARAMETER_MUST_BE_CONST!>a1 > a2<!>,
            <!ANNOTATION_PARAMETER_MUST_BE_CONST!>a1 == a2<!>
    ) val b = 1
}