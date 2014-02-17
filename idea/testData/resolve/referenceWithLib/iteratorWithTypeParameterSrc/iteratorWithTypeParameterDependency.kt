package dependency

class Foo<T> {
}

class FooIterator<T> {
}

fun <T> Foo<T>.iterator() = FooIterator<T>()
fun <T> FooIterator<T>.hasNext() = false
fun <T> FooIterator<T>.next() = throw IllegalStateException()
