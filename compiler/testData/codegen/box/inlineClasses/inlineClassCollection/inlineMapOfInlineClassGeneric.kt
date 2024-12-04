// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZArrayMap(val storage: IntArray) : Map<Z<Int>, Z<Int>> {
    override val size: Int
        get() = storage.size

    private class MapEntry(val i: Int, val si: Int): Map.Entry<Z<Int>, Z<Int>> {
        override val key: Z<Int> get() = Z(i)
        override val value: Z<Int> get() = Z(si)
    }

    private class MapEntrySet(val storage: IntArray) : AbstractSet<Map.Entry<Z<Int>, Z<Int>>>() {
        private inner class MyIterator : Iterator<Map.Entry<Z<Int>, Z<Int>>> {
            var index = 0
            override fun hasNext(): Boolean = index < size
            override fun next(): Map.Entry<Z<Int>, Z<Int>> = MapEntry(index, storage[index++])
        }

        override val size: Int
            get() = storage.size

        override fun iterator(): Iterator<Map.Entry<Z<Int>, Z<Int>>> = MyIterator()
    }

    override val entries: Set<Map.Entry<Z<Int>, Z<Int>>>
        get() = MapEntrySet(storage)

    override val keys: Set<Z<Int>>
        get() = (0 until size).mapTo(HashSet()) { Z(it) }

    override val values: Collection<Z<Int>>
        get() = storage.mapTo(ArrayList()) { Z(it) }

    override fun containsKey(key: Z<Int>): Boolean = key.x in (0 until size)

    override fun containsValue(value: Z<Int>): Boolean = storage.contains(value.x)

    override fun get(key: Z<Int>) = storage.getOrNull(key.x)?.let { Z(it) }

    override fun isEmpty(): Boolean = size > 0
}

fun box(): String {
    val zm = ZArrayMap(IntArray(5))

    zm.containsKey(Z(0))
    zm.containsValue(Z(0))
    zm[Z(0)]

    zm.containsKey(object {} as Any)
    zm.containsValue(object {} as Any)
    zm.get(object {} as Any)

    return "OK"
}