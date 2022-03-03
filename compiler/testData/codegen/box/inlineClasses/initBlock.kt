// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class SingleInitBlock(val s: String) {
    init {
        res = s
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class MultipleInitBlocks(val a: Any?) {
    init {
        res = "O"
    }
    init {
        res += "K"
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Lambda(val s: String) {
    init {
        val lambda = { res = s }
        lambda()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class FunLiteral(val s: String) {
    init {
        val funLiteral = fun() {
            res = s
        }
        funLiteral()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
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

OPTIONAL_JVM_INLINE_ANNOTATION
value class LocalFunction(val s: String) {
    init {
        fun local() {
            res = s
        }
        local()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
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

OPTIONAL_JVM_INLINE_ANNOTATION
value class Getter(val s: String) {
    init {
        res = ok
    }

    val ok: String
        get() = s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class GetterThis(val s: String) {
    init {
        res = this.ok
    }

    val ok: String
        get() = s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Method(val s: String) {
    init {
        res = ok(this)
    }

    fun ok(m: Method): String = m.s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class MethodThis(val s: String) {
    init {
        res = this.ok(this)
    }

    fun ok(m: MethodThis): String = m.s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineFun(val s: String) {
    init {
        res = ok()
    }

    inline fun ok(): String = s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineFunThis(val s: String) {
    init {
        res = this.ok()
    }

    inline fun ok(): String = s
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineClass(val s: String) {
    init {
        SingleInitBlock(s)
    }
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

    res = "FAIL 14"
    InlineClass("OK")
    if (res != "OK") return "FAIL 141: $res"

    return "OK"
}