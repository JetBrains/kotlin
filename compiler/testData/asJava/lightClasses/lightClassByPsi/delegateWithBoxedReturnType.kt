interface Base {
    fun int(): Int?
    fun long(): Long?
    fun boolean(): Boolean?
    fun char(): Char?

    val intProperty: Int?
    val longProperty: Long?
    val booleanProperty: Boolean?
    val charProperty: Char?
}

class Delegating(delegate: Base) : Base by delegate
