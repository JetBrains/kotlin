// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A<T>
fun <T> A<T>.foo(): Collection<T> = null!!

fun main(a: A<*>, a1: A<out CharSequence>) {
    // see KT-9571
    for (i in a.foo()) { }
    for (i: Any? in a.foo()) { }

    for (i in a1.foo()) { }
    for (i: CharSequence in a1.foo()) { }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, forLoop, funWithExtensionReceiver,
functionDeclaration, localProperty, nullableType, outProjection, propertyDeclaration, starProjection, typeParameter */
