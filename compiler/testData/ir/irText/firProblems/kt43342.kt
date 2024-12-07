// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// ^ Map has js specific methods
// IGNORE_BACKEND_K2: JS_IR

open class ControlFlowInfo<K, V>(val map: Map<K, V>): Map<K, V> by map

class StringFlowInfo(map: Map<String, String>): ControlFlowInfo<String, String>(map) {
    fun foo(info: StringFlowInfo) {
        keys
        info.keys
    }
}
