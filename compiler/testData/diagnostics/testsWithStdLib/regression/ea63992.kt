fun add(a: Int, b: Int) = a + b
interface A {
    fun <T> shuffle(x: List<T>): List<T>
    fun <T> foo(f : (List<T>) -> List<T>, x : List<T>)

fun f() : (Int, Int) -> Int = ::add
}