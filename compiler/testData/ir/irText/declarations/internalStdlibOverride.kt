// WITH_STDLIB
// KT-64692

// DUMP_IR_DIFFERENCE: JS_IR
// test data differs - JS stdlib has extra `asJsReadonlyMapView` fakeoverride from `public actual interface Map`.

class MyMap : AbstractMap<Int, Int>() {
    override val entries = emptySet<Map.Entry<Int, Int>>()

    // clash with stdlib internal function
    fun containsEntry(entry: Map.Entry<*, *>?) = false
}
