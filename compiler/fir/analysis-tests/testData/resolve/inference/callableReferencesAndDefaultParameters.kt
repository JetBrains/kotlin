// RUN_PIPELINE_TILL: BACKEND
class A {
    fun foo(s: String, flag: Boolean = true) {}
}

inline fun <T> T.myLet(block: (T) -> Unit) {}

fun test(a: A, s: String) {
    s.myLet(a::foo)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, nullableType, typeParameter */
