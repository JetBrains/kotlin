// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

class Test(val s: S) {
    constructor(x: String, s: S) : this(S(x + s.string))
}

fun box() = Test("O", S("K")).s.string