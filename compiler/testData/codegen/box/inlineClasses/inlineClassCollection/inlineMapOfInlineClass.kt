// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ZArrayMap(val storage: IntArray) : Map<Z, Z> {
    override val size: Int
        get() = storage.size

    private class MapEntry(val i: Int, val si: Int): Map.Entry<Z, Z> {
        override val key: Z get() = Z(i)
        override val value: Z get() = Z(si)
    }

    private class MapEntrySet(val storage: IntArray) : AbstractSet<Map.Entry<Z, Z>>() {
        private inner class MyIterator : Iterator<Map.Entry<Z, Z>> {
            var index = 0
            override fun hasNext(): Boolean = index < size
            override fun next(): Map.Entry<Z, Z> = MapEntry(index, storage[index++])
        }

        override val size: Int
            get() = storage.size

        override fun iterator(): Iterator<Map.Entry<Z, Z>> = MyIterator()
    }

    override val entries: Set<Map.Entry<Z, Z>>
        get() = MapEntrySet(storage)

    override val keys: Set<Z>
        get() = (0 until size).mapTo(HashSet()) { Z(it) }

    override val values: Collection<Z>
        get() = storage.mapTo(ArrayList()) { Z(it) }

    override fun containsKey(key: Z): Boolean = key.x in (0 until size)

    override fun containsValue(value: Z): Boolean = storage.contains(value.x)

    override fun get(key: Z) = storage.getOrNull(key.x)?.let { Z(it) }

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