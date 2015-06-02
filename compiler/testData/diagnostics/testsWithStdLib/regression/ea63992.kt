fun add(a: Int, b: Int) = a + b
interface A {
    fun shuffle<T>(x: List<T>): List<T>
    fun foo<T>(f : (List<T>) -> List<T>, x : List<T>)

fun f() : (Int, Int) -> Int = ::add
}