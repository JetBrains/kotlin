// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-61933

data class Bar(
    val foo: Foo<suspend () -> Unit>
)

data class Foo<out TCallback : Any>(
    val state: TCallback?,
)

fun usage(b: Boolean) {
    Bar(
        foo = Foo(
            state = if (b) { -> } else null,
        )
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, ifExpression, lambdaLiteral, nullableType, out,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
