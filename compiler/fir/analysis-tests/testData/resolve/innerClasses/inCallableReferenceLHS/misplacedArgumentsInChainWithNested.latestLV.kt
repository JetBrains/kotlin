// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

class Outer<T> {
    class Middle<B, A> {
        inner class Inner<C>
    }
}

fun Outer.Middle<String, Int>.Inner<Char>.foo() {
}

fun testWithThreeArgs() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Int>.Middle<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!><!>.Inner<Char>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Middle<!><!>.Inner<Char, String>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    Outer.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Middle<!>.Inner<Char, String, Int>::foo
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Char, String, Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Middle<!><!>.Inner::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    Outer.Middle<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>.Inner<Char, String>::foo
}

fun testWithFourArgs() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Any>.Middle<String, Int><!>.Inner<Char>::foo
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Int, Any>.Middle<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!><!>.Inner<Char>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    Outer.Middle<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Any><!>.Inner<Char>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, inner,
nestedClass, nullableType, typeParameter */
