// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

class DroppingDefaultParameters<T> {
    inner class A<K> {
        fun foo(x: Int, y: String = ""): String = "$x$y"
    }
    val a: A<String>.(Int) -> String = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<String><!>::foo
}

val a: DroppingDefaultParameters<String>.A<String>.(Int) -> String = DroppingDefaultParameters<String>.A<String>::foo
val b: DroppingDefaultParameters<String>.A<String>.(Int) -> String = DroppingDefaultParameters<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!><String, String><!>.A::foo
val c: DroppingDefaultParameters<String>.A<String>.(Int) -> String = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!>DroppingDefaultParameters<!>.A<String, String>::foo

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, nullableType,
propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */
