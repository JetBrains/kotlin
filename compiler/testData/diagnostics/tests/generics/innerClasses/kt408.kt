// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
interface T<E> {
    fun f() : E = null!!
}
open class A<X>() {
    inner class B() : T<X> {}
}

fun test() {
    val a = A<Int>()
    val b : A<Int>.B = a.B()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, inner, interfaceDeclaration,
localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
