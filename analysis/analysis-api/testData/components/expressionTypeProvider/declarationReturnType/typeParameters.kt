fun <T> id(t: T) = t
fun <C : Collection<*>> C.mySize = size
fun <T> foo(): Foo<T> = Foo()
class Foo<T> {
    val t: T
    fun getT(): T
    fun <R> getR(): R
}