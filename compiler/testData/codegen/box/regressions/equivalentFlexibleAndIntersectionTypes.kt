// WITH_RUNTIME

object FirstTest {
    fun <T> foo(a: Inv<out T>) {
        bar(a)
    }

    fun <T> bar(a: Inv<T?>) {}

    class Inv<T>
}

object SecondTest {
    fun <T> foo(elements: Inv<out T?>) {
        bar(elements)
    }

    fun <V> bar(a: Inv<V?>) {

    }

    class Inv<T>
}

fun box(): String {
    return "OK"
}