// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    fun foo(): CharSequence
}

interface B {
    fun foo(): String?
}

fun <T> test(x: T) where T : B, T : A {
    x.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
interfaceDeclaration, nullableType, typeConstraint, typeParameter, typeWithExtension */
