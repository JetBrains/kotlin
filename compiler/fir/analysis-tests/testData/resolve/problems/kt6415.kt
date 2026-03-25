// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6415
// FIR_DUMP

// KT-6415: kotlin.Nothing is substituted for type parameters if expected type is raw or star-projected
class Foo<T>(val block: (T) -> Boolean)

fun fooTest(x: Foo<*>) {}

fun bar() {
    fooTest(Foo { _: String -> true })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, starProjection, typeParameter */
