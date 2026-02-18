// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-66344, KT-82122
// LATEST_LV_DIFFERENCE

class C<U> {
    fun <T> foo() {
        class L

        val x = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>L<!>::toString
        val y = L<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int><!>::toString
        val z = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>L<Int, Long><!>::toString

        x(L())
        y(<!ARGUMENT_TYPE_MISMATCH!>L()<!>)
        z(<!ARGUMENT_TYPE_MISMATCH!>L()<!>)
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, localClass,
localProperty, nullableType, propertyDeclaration, typeParameter */
