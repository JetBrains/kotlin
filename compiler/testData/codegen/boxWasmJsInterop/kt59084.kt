// TARGET_BACKEND: WASM

inline class JsDynamic(val value: JsAny?)

val JsAny?.jsDyn: JsDynamic get() = JsDynamic(this)

fun test(): Boolean {
    val jsDynamic: JsAny? = 1.toJsNumber()
    val jsDyn = jsDynamic.jsDyn
    return jsDyn.value == jsDynamic
}

fun box(): String {
    if (!test()) {
        return "Fail"
    }
    return "OK"
}
