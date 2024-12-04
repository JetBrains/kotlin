interface List<out T : Any> {
    operator fun get(index: Int): T

    infix fun concat(other: List<T>): List<T>
}

typealias StringList = List<out String>
typealias AnyList = List<*>

abstract class AbstractList<out T : Any> : List<T>

class SomeList : AbstractList<Int>() {
    override fun get(index: Int): Int = 42

    override fun concat(other: List<Int>): List<Int> = this
}

fun <From, To> copyNotNull(from: List<From>, to: List<To>) where From : To, To : Any {
}
