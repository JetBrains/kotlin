// RUN_PIPELINE_TILL: FRONTEND

open class SomeClass<T> {

    class SomeImplementation : SomeClass<String>()

    inner class OtherImplementation<S: SomeClass<K>, K> {
        public fun foo(){}
    }

    fun testUnderscore() {
        OtherImplementation<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><SomeClass.SomeImplementation, _, String><!>()::foo
        OtherImplementation<SomeClass.SomeImplementation, _>()::foo
        OtherImplementation<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><SomeClass.SomeImplementation, _, <!OTHER_ERROR!>_<!>><!>()::foo
    }
}

fun testUnderscore() {
    <!CANNOT_INFER_PARAMETER_TYPE!>SomeClass<!>().OtherImplementation<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><SomeClass.SomeImplementation, _, String><!>()::foo
    SomeClass<Int>().OtherImplementation<SomeClass.SomeImplementation, _>()::foo
    SomeClass<Int>().OtherImplementation<SomeClass.SomeImplementation, String>()::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, nestedClass, nullableType,
typeConstraint, typeParameter */
