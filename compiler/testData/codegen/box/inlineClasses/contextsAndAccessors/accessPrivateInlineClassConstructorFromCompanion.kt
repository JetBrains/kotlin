// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Composed(val s: String) {
    private constructor(s1: String, s2: String) : this(s1 + s2)

    companion object {
        fun p1(s: String) = Composed("O", s)
    }
}

fun box() = Composed.p1("K").s