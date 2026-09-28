// TARGET_BACKEND: WASM
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")
import kotlin.reflect.KProperty

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

class Delegate<T>(val inner: T) { operator fun getValue(t: Any?, p: KProperty<*>): T = inner }
val prop: Data by (Delegate<Any>("str") as Delegate<Data>)
fun box() = expectCCE { prop.x }
