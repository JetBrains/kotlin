// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
sealed class CompletionPhase constructor(
    @JvmField
    val indicator: Any?
) {

    abstract fun newCompletionStarted(invocationCount: Int, repeated: Boolean): Int

    /** see doc of [CompletionPhase] */
    private object NoCompletionImpl: CompletionPhase(null) {
        override fun newCompletionStarted(invocationCount: Int, repeated: Boolean): Int {
            return invocationCount
        }

        override fun toString(): String {
            return "NoCompletion"
        }
    }

    companion object {
        @JvmField
        val NoCompletion: CompletionPhase = NoCompletionImpl
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nestedClass, nullableType,
objectDeclaration, override, primaryConstructor, propertyDeclaration, sealed, stringLiteral */
