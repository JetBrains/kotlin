// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

@JvmInline
value class A(val b: String) {
    override fun toString(): String =
        buildString { append(b) }
}

fun box() = A("OK").toString()
