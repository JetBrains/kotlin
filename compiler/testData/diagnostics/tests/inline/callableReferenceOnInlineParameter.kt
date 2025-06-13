// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
fun interface Warp<A, B> {
    fun apply(input: A): B

    companion object {
        inline fun <A, B> create(f: (A) -> B): Warp<A, B> = Warp(<!USAGE_IS_NOT_INLINABLE!>f<!>::invoke)
    }
}

/* GENERATED_FIR_TAGS: callableReference, companionObject, funInterface, functionDeclaration, functionalType, inline,
interfaceDeclaration, nullableType, objectDeclaration, typeParameter */
