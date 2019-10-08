// SmartSet

class SmartSet<T> private constructor() : AbstractSet<T>(), MutableSet<T> {
    companion object {
        private val ARRAY_THRESHOLD = 5

        @JvmStatic
        fun <T> create() = SmartSet<T>()

        @JvmStatic
        fun <T> create(set: Collection<T>): SmartSet<T> = TODO()
    }

    private var data: Any? = null

    override var size: Int = 0



    override fun iterator(): MutableIterator<T> = TODO()

    override fun add(element: T): Boolean  = TODO()

    override fun clear() {
        data = null
        size = 0
    }
}
