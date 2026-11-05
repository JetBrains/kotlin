interface BaseAny {
    fun int(): Any
    val intProperty: Any
}

interface BaseInt : BaseAny {
    override fun int(): Int
    override val intProperty: Int
}

class DelegatingInt(delegate: BaseInt) : BaseInt by delegate
