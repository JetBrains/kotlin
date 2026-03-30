// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-60225

class Klass<T: Klass<T>>

fun <T: Klass<T>> Klass<T>.foo() {}

fun main() {
    Klass().foo()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, typeConstraint,
typeParameter */
