private enum class MethodKind {
    INSTANCE,
    STATIC
}

private fun MethodKind.hasThis() = this == MethodKind.INSTANCE
