// WITH_STDLIB

class C {
    @Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
    value class IC1(val s: String)

    companion object {
        @Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
        value class IC2(val s: String)
    }
}

object O {
    @Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
    value class IC3(val s: String)
}

interface I {
    @Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
    value class IC4(val s: String)
}

fun box(): String {
    if (C.IC1("OK").s != "OK") return "FAIL 1"
    if (C.Companion.IC2("OK").s != "OK") return "FAIL 2"
    if (O.IC3("OK").s != "OK") return "FAIL 3"
    if (I.IC4("OK").s != "OK") return "FAIL 4"
    return "OK"
}