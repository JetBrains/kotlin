// WITH_STDLIB
// TARGET_BACKEND: WASM
// WASM_ALLOW_FQNAME_IN_KCLASS

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

enum class E { A }

sealed interface S
class A : S
class B : S

interface IFACE
interface IFACE_CS : IFACE, CharSequence

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

@Suppress("UNCHECKED_CAST")
fun <T> wrongBound2(): T where T : IFACE, T : CharSequence = "txt" as Any as T

fun tryCastGenericDoubleBound(): String? {
    val expected = "Cannot cast instance of kotlin.String to IFACE: incompatible types"
    try {
        wrongBound2<IFACE_CS>()
    } catch (cce: ClassCastException) {
        return if (cce.message != expected) "Expected $expected but actual <${cce.message}>" else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was thrown"
}

@Suppress("UNCHECKED_CAST")
fun <T> wrongArray(): Array<T> = intArrayOf(1) as Any as Array<T>

fun tryCastGenericArray(): String? {
    val expected = "Cannot cast instance of kotlin.IntArray to kotlin.Array: incompatible types"
    try {
        wrongArray<String>()
    } catch (cce: ClassCastException) {
        return if (cce.message != expected)
            "Expected $expected but actual <${cce.message}>"
        else null
    }
    return "Expected ClassCastException with message <$expected> but no exception was thrown"
}

inline fun <reified T : Any> nullAsT(): T = null as T

inline fun <reified T> expectOk(x: Any?): String? =
    try {
        x as T
        null
    } catch (cce: ClassCastException) {
        "Unexpected CCE: ${cce.message}"
    }

interface I
open class Base: I
class Derived: Base()

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
    tryCastGenericDoubleBound()?.let { return it }
    tryCastGenericArray()?.let { return it }
    tryCast<UInt>(42, "Cannot cast instance of kotlin.Int to kotlin.UInt: incompatible types")?.let { return it }
    tryCast<Int>(E.A, "Cannot cast instance of E to kotlin.Int: incompatible types")?.let { return it }
    tryCast<Unit>(42, "Cannot cast instance of kotlin.Int to kotlin.Unit: incompatible types")?.let { return it }
    val f = {}
    tryCast<(String) -> Int>(f, "Cannot cast instance of box\$lambda to kotlin.Function1: incompatible types")?.let { return it }
    tryCast<Array<String>>(intArrayOf(1), "Cannot cast instance of kotlin.IntArray to kotlin.Array: incompatible types")?.let { return it }
    val sf: suspend () -> Unit = suspend { }
    tryCast<(String) -> Int>(sf, "Cannot cast instance of box\$slambda to kotlin.Function1: incompatible types")?.let { return it }
    tryCast<IntArray>(arrayOf(1), "Cannot cast instance of kotlin.Array to kotlin.IntArray: incompatible types")?.let { return it }
    tryCast<Array<Int>>(intArrayOf(1), "Cannot cast instance of kotlin.IntArray to kotlin.Array: incompatible types")?.let { return it }
    val u: Any = 1u
    tryCast<Int>(u, "Cannot cast instance of kotlin.UInt to kotlin.Int: incompatible types")?.let { return it }
    tryCast<B>(A(), "Cannot cast instance of A to B: incompatible types")?.let { return it }
    tryCast<IntArray>(enumValues<E>(), "Cannot cast instance of kotlin.Array to kotlin.IntArray: incompatible types")?.let { return it }
    val propRef = String::length
    tryCast<() -> Int>(propRef, "Cannot cast instance of kotlin.wasm.internal.KProperty1Impl to kotlin.Function0: incompatible types")?.let { return it }
    tryCast<Derived>(Base(), "Cannot cast instance of Base to Derived: incompatible types")?.let { return it }
    return "OK"
}