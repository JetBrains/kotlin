package test

public abstract class A<T>

inline fun <reified T> foo1(): A<T> {
    return object : A<T>() {

    }
}

fun<T> bar(x: T, block: (T) -> Boolean): Boolean = block(x)

inline fun <reified T> foo2(x: Any): Boolean {
    return bar(x) { it is T }
}

inline fun <reified T> foo3(x: Any, y: Any): Boolean {
    return bar(x) { it is T && y is T }
}
