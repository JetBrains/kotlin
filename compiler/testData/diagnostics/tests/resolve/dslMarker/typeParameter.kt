// RUN_PIPELINE_TILL: FRONTEND
@DslMarker
annotation class Dsl

@Dsl interface Foo {
    fun foo()
}
@Dsl interface Bar

fun <T : Foo> test(foo: T, bar: Bar) {
    with(foo) {
        with(bar) {
            <!DSL_SCOPE_VIOLATION!>foo<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral, typeConstraint,
typeParameter */
