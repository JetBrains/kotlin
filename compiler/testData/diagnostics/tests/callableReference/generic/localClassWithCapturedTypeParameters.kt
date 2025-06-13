// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68350

fun <T, K> test() {
    class InnerClass(var x: Int, val y: T)

    val list: List<InnerClass> = listOf()
    list.sortedBy(InnerClass::x)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localClass, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
