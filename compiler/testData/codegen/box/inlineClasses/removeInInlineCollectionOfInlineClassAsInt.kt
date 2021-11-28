// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z2(val x: Z)

fun z2(x: Int) = Z2(Z(x))

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ZMutableCollection(private val ms: MutableCollection<Z>) : MutableCollection<Z> {
    override fun add(element: Z): Boolean = ms.add(element)
    override fun addAll(elements: Collection<Z>): Boolean = ms.addAll(elements)
    override fun clear() { ms.clear() }
    override fun iterator(): MutableIterator<Z> = ms.iterator()
    override fun remove(element: Z): Boolean = ms.remove(element)
    override fun removeAll(elements: Collection<Z>): Boolean = ms.removeAll(elements)
    override fun retainAll(elements: Collection<Z>): Boolean = ms.retainAll(elements)
    override val size: Int get() = ms.size
    override fun contains(element: Z): Boolean = ms.contains(element)
    override fun containsAll(elements: Collection<Z>): Boolean = ms.containsAll(elements)
    override fun isEmpty(): Boolean = ms.isEmpty()
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z2MutableCollection(private val ms: MutableCollection<Z2>) : MutableCollection<Z2> {
    override fun add(element: Z2): Boolean = ms.add(element)
    override fun addAll(elements: Collection<Z2>): Boolean = ms.addAll(elements)
    override fun clear() { ms.clear() }
    override fun iterator(): MutableIterator<Z2> = ms.iterator()
    override fun remove(element: Z2): Boolean = ms.remove(element)
    override fun removeAll(elements: Collection<Z2>): Boolean = ms.removeAll(elements)
    override fun retainAll(elements: Collection<Z2>): Boolean = ms.retainAll(elements)
    override val size: Int get() = ms.size
    override fun contains(element: Z2): Boolean = ms.contains(element)
    override fun containsAll(elements: Collection<Z2>): Boolean = ms.containsAll(elements)
    override fun isEmpty(): Boolean = ms.isEmpty()
}

fun box(): String {
    val zc1 = ZMutableCollection(mutableListOf(Z(1), Z(2), Z(3)))
    zc1.remove(Z(1))
    if (Z(1) in zc1) throw AssertionError("Z(1) in zc1")

    val zc2 = Z2MutableCollection(mutableListOf(z2(1), z2(2), z2(3)))
    zc2.remove(z2(1))
    if (z2(1) in zc2) throw AssertionError("z2(1) in zc2")

    return "OK"
}