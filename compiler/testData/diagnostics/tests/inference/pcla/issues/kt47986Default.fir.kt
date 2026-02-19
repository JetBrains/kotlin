// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

fun <K> Foo<K>.bar(x: Int = 1) {}

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE!>buildFoo<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
