// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

abstract class Base(val x: S)

class Test(x: S) : Base(x)

fun box() = Test(S("OK")).x.string