// KT-307 Unresolved reference

open class AL {
    fun get(i : Int) : Any? = i
}

interface ALE<T> : <!INTERFACE_WITH_SUPERCLASS!>AL<!> {
    fun getOrNull(index: Int, value: T) : T {
        return get(index) as? T ?: value
    }
}