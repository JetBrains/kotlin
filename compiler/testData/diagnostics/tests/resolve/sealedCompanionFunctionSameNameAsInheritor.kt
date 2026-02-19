// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
sealed class SourceLocation {
    object NoLocation : SourceLocation()
    companion object {
        fun NoLocation(description: String): SourceLocation = NoLocation
    }
}

fun test() {
    SourceLocation.NoLocation("")
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nestedClass, objectDeclaration, sealed,
stringLiteral */
