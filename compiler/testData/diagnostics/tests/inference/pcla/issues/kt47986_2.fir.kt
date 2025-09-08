// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

fun <L> Foo<L>.bar() {}

fun <K> id(x: K) = x

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE!>buildFoo<!> { // can't infer
        val y = id(::<!CANNOT_INFER_PARAMETER_TYPE!>bar<!>)
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
