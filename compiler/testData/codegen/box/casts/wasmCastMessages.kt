// WITH_STDLIB
// TARGET_BACKEND: WASM

import kotlin.js.Promise

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

fun tryCastToNothing(x: Any?, expected: String): String? {
    try {
        x as Nothing
    } catch (cce: ClassCastException) {
        return if (cce.message != expected) "Expected $expected but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was throwed"
}

fun tryCastToNNothing(x: Any?, expected: String): String? {
    try {
        x as Nothing?
    } catch (cce: ClassCastException) {
        return if (cce.message != expected) "Expected $expected but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was throwed"
}

interface IFACE
fun <T : IFACE>wrongResultOfT(): T = Any() as T

fun tryCastGeneric(): String? {
    val expected = "Cannot cast instance of kotlin.Any to IFACE: incompatible types"
    try {
        wrongResultOfT()
    } catch (cce: ClassCastException) {
        return if (cce.message != expected) "Expected $expected but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was throwed"
}

fun box(): String {
    tryCast<String>(42, "Cannot cast instance of kotlin.Int to kotlin.String: incompatible types")?.let { return it }
    tryCast<String?>(42, "Cannot cast instance of kotlin.Int to kotlin.String?: incompatible types")?.let { return it }
    tryCast<String>(null, "Cannot cast null to kotlin.String: target type is non-nullable")?.let { return it }
    tryCastToNNothing(42, "Expected null (Nothing?), got an instance of kotlin.Int")?.let { return it }
    tryCast<CharSequence>(42, "Cannot cast instance of kotlin.Int to kotlin.CharSequence: incompatible types")?.let { return it }
    tryCast<String>(Promise.resolve(null), "Cannot cast instance of Promise to kotlin.String: incompatible types")?.let { return it }
    tryCast<Promise<*>>(42, "Cannot cast instance of kotlin.Int to Promise: incompatible types")?.let { return it }
    tryCastToJsAny(null)?.let { return it }
    tryCastToNothing(42, "Cannot cast instance of kotlin.Int to kotlin.Nothing: incompatible types")?.let { return it }
    tryCastToNothing(null, "Cannot cast null to kotlin.Nothing: target type is non-nullable")?.let { return it }
    tryCastGeneric()?.let { return it }
    return "OK"

}