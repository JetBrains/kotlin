// RUN_PIPELINE_TILL: BACKEND
private enum class MethodKind {
    INSTANCE,
    STATIC
}

private fun MethodKind.hasThis() = this == MethodKind.INSTANCE

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, funWithExtensionReceiver, functionDeclaration,
thisExpression */
