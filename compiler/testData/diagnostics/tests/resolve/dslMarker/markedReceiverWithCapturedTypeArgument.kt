// RUN_PIPELINE_TILL: CODEGEN
@DslMarker
annotation class AnnMarker

@AnnMarker
class Inv<T> {
    fun bar() {}
}

fun Inv<*>.foo() {
    bar()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, funWithExtensionReceiver, functionDeclaration,
nullableType, starProjection, typeParameter */
