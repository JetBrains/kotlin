// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-6415

// KT-6415: kotlin.Nothing is substituted for type parameters if expected type is raw or star-projected
class Foo<T>(val block: (T) -> Boolean)

fun fooTest(x: Foo<*>) {}

fun bar() {
    fooTest(<!CANNOT_INFER_PARAMETER_TYPE!>Foo<!> { <!CANNOT_INFER_PARAMETER_TYPE!>s<!> -> true })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, starProjection, typeParameter */
