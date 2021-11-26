// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

class Outer(val s1: S) {
    inner class Inner(val s2: S) {
        val test = s1.string + s2.string
    }
}

fun box() = Outer(S("O")).Inner(S("K")).test