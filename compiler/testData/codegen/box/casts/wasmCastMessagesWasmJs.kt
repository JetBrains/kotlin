// WITH_STDLIB
// TARGET_BACKEND: WASM_JS

import kotlin.js.Promise
import kotlin.reflect.KProperty1

inline fun <reified T> tryCast(x: Any?, expected: String?): String? {
    try {
        x as T
    } catch (cce: ClassCastException) {
        return if (cce.message != expected) "Expected <$expected> but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was throwed"
}

fun tryCastToJsAny(x: Any?): String? {
    try {
        x as JsAny
    } catch (cce: ClassCastException) {
        return if (cce.message != null) "Expected null but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message null but no exception was throwed"
}

fun box(): String {
    tryCast<String>(Promise.resolve(null), "Cannot cast instance of Promise to kotlin.String: incompatible types")?.let { return it }
    tryCast<Promise<*>>(42, "Cannot cast instance of kotlin.Int to Promise: incompatible types")?.let { return it }
    tryCastToJsAny(null)?.let { return it }
    return "OK"
}