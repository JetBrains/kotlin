// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A<TA>

class B<TB> : A<TB & Any>()

fun accept(a: A<String>) {}

fun test() {
    val b = B<String?>()
    accept(b)
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, localProperty, nullableType, propertyDeclaration,
typeParameter */
