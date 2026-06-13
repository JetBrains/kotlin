// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

class WithParenthesizes<T> {
    inner class A<K> {
        fun foo() {}
    }
}

fun testWithParenthesizes() {
    (WithParenthesizes<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!><String, Int><!>.A)::foo
    (WithParenthesizes<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!><String, Int><!>).A::foo
    (<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!>WithParenthesizes<!>.A<String, Int>)::foo
    (<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!>WithParenthesizes<!>).A<String, Int>::foo
    (WithParenthesizes<String>).A<Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, nullableType, typeParameter */
