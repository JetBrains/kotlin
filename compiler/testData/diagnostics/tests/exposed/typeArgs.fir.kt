internal open class My

abstract class Your {
    // invalid, List<My> is effectively internal
    abstract fun give(): List<My>
}

// invalid, List<My> is effectively internal
interface His: List<My>

// invalid, My is internal
interface Generic<E: My>

interface Our {
    // invalid, Generic<My> is effectively internal
    fun foo(): Generic<*>
}