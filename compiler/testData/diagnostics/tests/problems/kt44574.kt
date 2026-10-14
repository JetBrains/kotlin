// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-44574

sealed class Base<T : Any> {
    class WithArgument<T : Any>(val arg: T) : Base<T>()
    class WithoutArgument<T : Any> : Base<T>()
}

fun test() {
    // Inference should pass without type mismatches and uninferred parameters
    listOf(
        listOf(
            Base.WithoutArgument(),
            Base.WithArgument(4),
            Base.WithoutArgument(),
        )
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nestedClass, primaryConstructor,
propertyDeclaration, sealed, typeConstraint, typeParameter */
