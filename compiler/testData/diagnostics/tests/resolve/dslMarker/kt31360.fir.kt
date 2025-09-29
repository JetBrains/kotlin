// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class MyDsl<!>

@MyDsl
interface Scope<A, B> {
    val something: A
    val value: B
}
fun scoped1(block: Scope<Int, String>.() -> Unit) {}
fun scoped2(block: Scope<*, String>.() -> Unit) {}

val <T> Scope<*, T>.property: T get() = value

fun f() {
    scoped1 {
        value
        property
    }
    scoped2 {
        value
        property
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, functionalType, getter, interfaceDeclaration,
lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, starProjection, typeParameter,
typeWithExtension */
