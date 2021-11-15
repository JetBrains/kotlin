// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class SingleInitBlock(val s: String) {
    init {
        res = s
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class MultipleInitBlocks(val a: Any?) {
    init {
        res = "O"
    }
    init {
        res += "K"
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Lambda(val s: String) {
    init {
        val lambda = { res = s }
        lambda()
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class FunLiteral(val s: String) {
    init {
        val funLiteral = fun() {
            res = s
        }
        funLiteral()
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ObjectLiteral(val s: String) {
    init {
        val objectLiteral = object {
            fun run() {
                res = s
            }
        }
        objectLiteral.run()
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class LocalFunction(val s: String) {
    init {
        fun local() {
            res = s
        }
        local()
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class LocalClass(val s: String) {
    init {
        class Local {
            fun run() {
                res = s
            }
        }
        Local().run()
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Getter(val s: String) {
    init {
        res = ok
    }

    val ok: String
        get() = s
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GetterThis(val s: String) {
    init {
        res = this.ok
    }

    val ok: String
        get() = s
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Method(val s: String) {
    init {
        res = ok(this)
    }

    fun ok(m: Method): String = m.s
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class MethodThis(val s: String) {
    init {
        res = this.ok(this)
    }

    fun ok(m: MethodThis): String = m.s
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineFun(val s: String) {
    init {
        res = ok()
    }

    inline fun ok(): String = s
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineFunThis(val s: String) {
    init {
        res = this.ok()
    }

    inline fun ok(): String = s
}

var res: String = "FAIL"

fun box(): String {
    SingleInitBlock("OK")
    if (res != "OK") return "FAIL 1: $res"

    res = "FAIL 2"
    MultipleInitBlocks(null)
    if (res != "OK") return "FAIL 21: $res"

    res = "FAIL 3"
    Lambda("OK")
    if (res != "OK") return "FAIL 31: $res"

    res = "FAIL 4"
    FunLiteral("OK")
    if (res != "OK") return "FAIL 41: $res"

    res = "FAIL 5"
    ObjectLiteral("OK")
    if (res != "OK") return "FAIL 51: $res"

    res = "FAIL 6"
    LocalFunction("OK")
    if (res != "OK") return "FAIL 61: $res"

    res = "FAIL 7"
    LocalClass("OK")
    if (res != "OK") return "FAIL 71: $res"

    res = "FAIL 8"
    Getter("OK")
    if (res != "OK") return "FAIL 81: $res"

    res = "FAIL 9"
    GetterThis("OK")
    if (res != "OK") return "FAIL 91: $res"

    res = "FAIL 10"
    Method("OK")
    if (res != "OK") return "FAIL 101: $res"

    res = "FAIL 11"
    MethodThis("OK")
    if (res != "OK") return "FAIL 111: $res"

    res = "FAIL 12"
    InlineFun("OK")
    if (res != "OK") return "FAIL 121: $res"

    res = "FAIL 13"
    InlineFunThis("OK")
    if (res != "OK") return "FAIL 131: $res"

    return "OK"
}