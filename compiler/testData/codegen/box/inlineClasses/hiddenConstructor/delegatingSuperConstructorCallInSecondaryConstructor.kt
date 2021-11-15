// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

abstract class Base(val x: S)

class Test : Base {
    constructor() : super(S("OK"))
}

fun box() = Test().x.string