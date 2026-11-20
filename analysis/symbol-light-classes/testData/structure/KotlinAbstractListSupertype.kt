package test.pkg

class ItemList<T> : AbstractList<T?>() {
    override fun get(index: Int): T? = null
    override val size: Int get() = 0
}
