// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

class Outer private constructor(val s: S) {
    class Nested {
        fun test(s: S) = Outer(s)
    }
}

fun box() = Outer.Nested().test(S("OK")).s.string