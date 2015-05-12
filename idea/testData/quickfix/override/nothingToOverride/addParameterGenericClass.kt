// "Change function signature to 'fun f(a: Int, x: T)'" "true"
interface A<R> {
    fun f(a: Int, b: R)
}

class B<T> : A<T> {
    <caret>override fun f(x: T) {}
}
