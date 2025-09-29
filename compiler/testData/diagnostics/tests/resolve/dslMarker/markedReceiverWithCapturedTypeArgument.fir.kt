// RUN_PIPELINE_TILL: BACKEND
@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class AnnMarker<!>

@AnnMarker
class Inv<T> {
    fun bar() {}
}

fun Inv<*>.foo() {
    bar()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, funWithExtensionReceiver, functionDeclaration,
nullableType, starProjection, typeParameter */
