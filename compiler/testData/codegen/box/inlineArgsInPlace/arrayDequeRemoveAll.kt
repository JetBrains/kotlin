// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464, KT-63984

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
class ArrayDeque<E> : AbstractMutableList<E> {
    private var head: Int = 0
    private var elementData: Array<Any?>

    override var size: Int = 0
        private set

    public constructor(elements: Collection<E>) {
        elementData = elements.toTypedArray()
        size = elementData.size
        if (elementData.isEmpty()) elementData = emptyElementData
    }


    override fun add(index: Int, element: E) {
        TODO()
    }

    override fun removeAt(index: Int): E {
        TODO()
    }

    override fun set(index: Int, element: E): E {
        TODO()
    }

    override fun get(index: Int): E {
        return internalGet(internalIndex(index))
    }

    @kotlin.internal.InlineOnly
    private inline fun internalGet(internalIndex: Int): E {
        @Suppress("UNCHECKED_CAST")
        return elementData[internalIndex] as E
    }

    private fun positiveMod(index: Int): Int = if (index >= elementData.size) index - elementData.size else index

    private fun negativeMod(index: Int): Int = if (index < 0) index + elementData.size else index

    @kotlin.internal.InlineOnly
    private inline fun internalIndex(index: Int): Int = positiveMod(head + index)

    private fun incremented(index: Int): Int = if (index == elementData.lastIndex) 0 else index + 1

    public override fun removeAll(elements: Collection<E>): Boolean = filterInPlace { !elements.contains(it) }

    private inline fun filterInPlace(predicate: (E) -> Boolean): Boolean {
        if (this.isEmpty() || elementData.isEmpty())
            return false

        val tail = internalIndex(size)
        var newTail = head
        var modified = false

        if (head < tail) {
            for (index in head until tail) {
                val element = elementData[index]

                if (predicate(element as E))
                    elementData[newTail++] = element
                else
                    modified = true
            }

            elementData.fill(null, newTail, tail)

        } else {
            for (index in head until elementData.size) {
                val element = elementData[index]
                elementData[index] = null

                if (predicate(element as E))
                    elementData[newTail++] = element
                else
                    modified = true
            }

            newTail = positiveMod(newTail)

            for (index in 0 until tail) {
                val element = elementData[index]
                elementData[index] = null

                if (predicate(element as E)) {
                    elementData[newTail] = element
                    newTail = incremented(newTail)
                } else {
                    modified = true
                }
            }
        }
        if (modified)
            size = negativeMod(newTail - head)

        return modified
    }

    internal companion object {
        private val emptyElementData = emptyArray<Any?>()
    }
}


fun box(): String {
    val ad = ArrayDeque(listOf("X", "Z", "O", "K"))
    ad.removeAll(listOf("X", "Z"))
    return ad[0] + ad[1]
}
