// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-66344, KT-82122
// LATEST_LV_DIFFERENCE

class C<U> {
    fun <T> foo() {
        class L

        val x = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("1; class L<Outer(T), Outer(U)> : Any")!>L<!>::toString
        val y = L<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING("0; class L<Outer(T), Outer(U)> : Any")!><Int><!>::toString
        val z = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("1; class L<Outer(T), Outer(U)> : Any")!>L<Int, Long><!>::toString

        x(L())
        y(<!ARGUMENT_TYPE_MISMATCH("L<T (of fun <T> foo), U (of class C<U>)>; L<Int, *>")!>L()<!>)
        z(<!ARGUMENT_TYPE_MISMATCH("L<T (of fun <T> foo), U (of class C<U>)>; L<Int, Long>")!>L()<!>)
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, localClass,
localProperty, nullableType, propertyDeclaration, typeParameter */
