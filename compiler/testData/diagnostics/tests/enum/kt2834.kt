// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
private enum class MethodKind {
    INSTANCE,
    STATIC
}

private fun MethodKind.hasThis() = this == MethodKind.INSTANCE
