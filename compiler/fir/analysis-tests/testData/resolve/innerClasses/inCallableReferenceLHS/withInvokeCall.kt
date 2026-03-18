// RUN_PIPELINE_TILL: FRONTEND

class Factory<T> {
    inner class A<K> {
        operator fun <T> invoke(): Factory<T>.A<K> = TODO()
        fun foo() {}
    }
}

val a = Factory<Int>().A<String>()<String>()::foo
val b = Factory<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>().<!CANNOT_INFER_PARAMETER_TYPE!>A<!>()<String>()::foo
val c = <!CANNOT_INFER_PARAMETER_TYPE!>Factory<Int>().A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>()()<!>::foo
val d = Factory<Int>().<!CANNOT_INFER_PARAMETER_TYPE!>A<!>()<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>()::foo
val e = <!CANNOT_INFER_PARAMETER_TYPE!>Factory<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String, String><!>().<!CANNOT_INFER_PARAMETER_TYPE!>A<!>()()<!>::foo

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, nullableType, operator,
propertyDeclaration, typeParameter */
