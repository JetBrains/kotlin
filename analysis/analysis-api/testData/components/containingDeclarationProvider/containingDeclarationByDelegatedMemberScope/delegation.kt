interface A<T> {
    fun x(): T
    fun y()
    val c: T
    val c: Int
}

class B<caret>B(a: A<Int>): A<Int> by a