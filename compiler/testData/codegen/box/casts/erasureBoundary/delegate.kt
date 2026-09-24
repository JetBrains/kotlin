// TARGET_BACKEND: WASM
// WITH_STDLIB
// Heap pollution must be detected where a value produced at an erased generic type is narrowed back
// to a substituted type (an erasure boundary): with a ClassCastException, not a Wasm trap.
// The result of an erased generic `getValue` is narrowed to the delegated property type.
@file:Suppress("UNCHECKED_CAST")
import kotlin.reflect.KProperty

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

class Delegate<T>(val inner: T) { operator fun getValue(t: Any?, p: KProperty<*>): T = inner }
val prop: Data by (Delegate<Any>("str") as Delegate<Data>)
fun box() = expectCCE { prop.x }
