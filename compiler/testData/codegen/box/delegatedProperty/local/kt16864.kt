// IGNORE_BACKEND: JS_IR

object Whatever {
    operator fun getValue(thisRef: Any?, prop: Any?) = "OK"
}

fun box(): String {
    val key by Whatever
    return {
        object {
            val keys = key
        }.keys
    } ()
}