// FIR_IDENTICAL
// WITH_STDLIB
// ENABLE_IR_FAKE_OVERRIDE_GENERATION
// KT-64692

// test data differs - no getOrDefault in AbstractMap for non-jvm.
// IGNORE_BACKEND: NATIVE, JS, JS_IR, JS_IR_ES6, WASM

class MyMap : AbstractMap<Int, Int>() {
    override val entries = emptySet<Map.Entry<Int, Int>>()

    // clash with stdlib internal function
    fun containsEntry(entry: Map.Entry<*, *>?) = false
}