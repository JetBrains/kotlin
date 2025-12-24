// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^ JS backend doesn't throw null pointer exception on null from JS side for non-null type

inline fun checkNPE(body: () -> Unit) {
    var throwed = false
    try {
        body()
    } catch (e: NullPointerException) {
        throwed = true
    }
    check(throwed)
}

fun notNull2String(): String = js("null")

external interface ExternRef

fun notNull2ExternRef(): ExternRef = js("null")

class StructRefImpl
typealias StructRef = JsReference<StructRefImpl>

fun notNull2StructRef(x: StructRef): StructRef = js("null")

fun notNull2Int(): Int = js("null")

fun notNull2Short(): Short = js("null")

fun notNull2Float(): Float = js("null")

fun notNull2Boolean(): Boolean = js("null")

fun box(): String {
    val structRef = StructRefImpl().toJsReference()

    checkNPE { notNull2String() }
    checkNPE { notNull2ExternRef() }
    checkNPE { notNull2StructRef(structRef) }

    check(notNull2Int() == 0)
    check(notNull2Short() == 0.toShort())
    check(notNull2Float() == 0.0f)
    check(notNull2Boolean() == false)

    return "OK"
}
