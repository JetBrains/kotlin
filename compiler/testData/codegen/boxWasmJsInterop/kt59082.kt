// WITH_STDLIB
// LANGUAGE: +AllowAnyAsAnActualTypeForExpectInterface

public external interface CanvasFillRule : JsAny {
    companion object
}

public inline val CanvasFillRule.Companion.EVENODD: CanvasFillRule get() =
    "evenodd".toJsString().unsafeCast<CanvasFillRule>()

public inline val CanvasFillRule.Companion.ODDEVEN: CanvasFillRule get() =
    "oddeven".toJsString().unsafeCast<CanvasFillRule>()

public inline fun CanvasFillRule.Companion.Gen(arg: String): CanvasFillRule {
    return "$arg".toJsString().unsafeCast<CanvasFillRule>()
}

fun box(): String {
    if (CanvasFillRule.EVENODD.toString() != "evenodd") return "Fail 1"
    if (CanvasFillRule.Gen("oddeven").toString() != "oddeven") return "Fail 2"

    when (CanvasFillRule.Gen("oddeven")) {
        CanvasFillRule.Companion.EVENODD -> return "Fail 3"
        CanvasFillRule.Companion.ODDEVEN -> {
        }
        else -> return "Fail 4"
    }
    return "OK"
}
