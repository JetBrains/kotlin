// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class GenericClass<out T>(val value: T) {
    public fun <P> foo(extension: T.() -> P) {}
}

public fun <E> GenericClass<List<E>>.bar() {
    foo( { listIterator() })
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
nullableType, out, primaryConstructor, propertyDeclaration, typeParameter, typeWithExtension */
