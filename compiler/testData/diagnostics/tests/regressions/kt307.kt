// KT-307 Unresolved reference

open class AL {
    fun get(i : Int) : Any? = i
}

trait ALE<T> : AL {
fun getOrNull(index: Int, value: T) : T {
return <!UNCHECKED_CAST!>get(index) as? T<!> ?: value
}
}