interface List<out T : Any> {
    operator fun get(index: Int): T

    infix fun concat(other: List<T>): List<T>
}

typealias StringList = List<out String>
typealias AnyList = List<*>

abstract class AbstractList<out T : Any> : List<T>

//               constructor AbstractList<T : Any>()
//               │
class SomeList : AbstractList<Int>() {
//                                    Int
//                                    │ Int
//                                    │ │
    override fun get(index: Int): Int = 42

//                                                   SomeList
//                                                   │
    override fun concat(other: List<Int>): List<Int> = this
}
