// TARGET_BACKEND: WASM

@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface CanvasFillRule : JsAny {
    companion object
}

public inline val CanvasFillRule.Companion.EVENODD: CanvasFillRule get() =
    "evenodd".toJsString().unsafeCast<CanvasFillRule>()

fun box(): String {
    if (CanvasFillRule.EVENODD.toString() != "evenodd") return "Fail 1"
    return "OK"
}
