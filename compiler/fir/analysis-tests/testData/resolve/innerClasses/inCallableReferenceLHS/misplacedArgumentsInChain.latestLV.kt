// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

class Outer<A> {
    inner class Middle<B> {
        inner class Inner<C>
    }
}

fun Outer<Int>.Middle<String>.Inner<Char>.foo() {
}

fun test() {
    Outer<Int>.Middle<String>.Inner<Char>::foo
    Outer<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Middle<!>.Inner<Char, String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.Middle.Inner<Char, String, Int>::foo
    Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Char, String, Int><!>.Middle.Inner::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.Middle<Int>.Inner<Char, String>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, inner,
nullableType, typeParameter */
