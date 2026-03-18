// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24994

// KT-24994: Class literal reference is not allowed to a local class inside generic function
fun test1() {
    class A
    class B<I>
    val a = A::class
    val b = B::class
}

fun <Whatever> test2() {
    class A
    class B<I>
    val a = A::class
    val b = B::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, localClass, localProperty, nullableType,
propertyDeclaration, starProjection, typeParameter */
