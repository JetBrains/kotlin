// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-61844
// FIR_DUMP

class Foo<I, out O>(
    private val transformer: (I) -> O,
) {
    fun <I, O> transform(foo: Foo<I, O>, bar: I) {
        foo.transformer(bar)
        foo.transformer.invoke(bar)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, nullableType, out, primaryConstructor,
propertyDeclaration, typeParameter */
