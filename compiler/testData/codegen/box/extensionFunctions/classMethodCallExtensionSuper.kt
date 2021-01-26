// KT-42176
interface Top<D>{
    fun getData(): D
    fun toString(data: D): String
}

fun <D> Top<D>.getString() = toString(getData())

abstract class DefaultImpl: Top<Int>{
    override fun toString(data: Int): String = data.toString()
}

class Bottom(val data: Int): DefaultImpl() {
    override fun getData(): Int =  data
}

fun box(): String {
    val bottom = Bottom(10).getString()
    if (bottom != "10") return "fail: $bottom"

    return "OK"
}