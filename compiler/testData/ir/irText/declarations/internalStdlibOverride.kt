// FIR_IDENTICAL
// WITH_STDLIB
// KT-64692

// test data differs - JS stdlib has extra `asJsReadonlyMapView` fakeoverride from `public actual interface Map`.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

class MyMap : AbstractMap<Int, Int>() {
    override val entries = emptySet<Map.Entry<Int, Int>>()

    // clash with stdlib internal function
    fun containsEntry(entry: Map.Entry<*, *>?) = false
}
