// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

class Test(val x: S, val y: S) {
    constructor(x: S) : this(x, S("K"))

    val test = x.string + y.string
}

fun box() = Test(S("O")).test