// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

open class ControlFlowInfo<K, V>(val map: Map<K, V>): Map<K, V> by map

class StringFlowInfo(map: Map<String, String>): ControlFlowInfo<String, String>(map) {
    fun foo(info: StringFlowInfo) {
        keys
        info.keys
    }
}
