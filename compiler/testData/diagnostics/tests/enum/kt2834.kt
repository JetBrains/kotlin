private enum class MethodKind {
    INSTANCE,
    STATIC
}

fun MethodKind.hasThis() = this == MethodKind.INSTANCE
