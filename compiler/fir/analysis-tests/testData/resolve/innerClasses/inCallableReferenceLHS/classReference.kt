// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

class ClassReference<T> {
    inner class A<K> {
        inner class DeepInner

        fun foo() {
            A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!><K, T><!>::DeepInner::class
            <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<K><!>::DeepInner::class
        }
    }
}

fun <T, K> testClassReference() {
    ClassReference<Int>.A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    ClassReference<Int>.A<Int>::DeepInner::class
    ClassReference<T>.A<K>::DeepInner::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>ClassReference<Int, Int>.A<!>::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!>ClassReference<!>.A<K, T>::DeepInner::class
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, inner, nullableType,
typeParameter */
