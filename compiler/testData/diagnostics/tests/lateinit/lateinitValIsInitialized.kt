// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +LateinitVals

lateinit val foo: String

fun test(bar: Bar) {
    ::foo.<!LATEINIT_INTRINSIC_CALL_ON_LATEINIT_VAL!>isInitialized<!>
    bar::x.<!LATEINIT_INTRINSIC_CALL_ON_LATEINIT_VAL!>isInitialized<!>
}

class Bar {
    lateinit val x: String

    fun test() {
        ::foo.<!LATEINIT_INTRINSIC_CALL_ON_LATEINIT_VAL!>isInitialized<!>
        ::x.<!LATEINIT_INTRINSIC_CALL_ON_LATEINIT_VAL!>isInitialized<!>
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, lateinit, propertyDeclaration */
