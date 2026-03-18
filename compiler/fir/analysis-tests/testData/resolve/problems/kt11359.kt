// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-11359

// KT-11359: Overloaded method resolution with generics is incorrect
class A<T> {
    fun bar(t: T) {}
    fun bar(a: A<T>) {}
}

fun test(a: A<Any>, b: A<Any>) {
    a.bar(b)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter */
