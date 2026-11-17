// WITH_STDLIB
// LANGUAGE: +AllowAnyAsAnActualTypeForExpectInterface
// ISSUE: KT-76462

@JsName("Object")
public external interface CanvasFillRule : JsAny {
    companion object
}

public inline val CanvasFillRule.Companion.EVENODD: CanvasFillRule get() =
    "evenodd".toJsString().unsafeCast<CanvasFillRule>()

fun box(): String {
    if (CanvasFillRule.EVENODD.toString() != "evenodd") return "Fail 1"
    return "OK"
}
