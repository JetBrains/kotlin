fun <T : Any> foo(elements: Inv<out T?>)  {
    bar(elements)
}

fun <V : Any> bar(a: Inv<out V?>) {}

class Inv<T>

fun box(): String {
    return "OK"
}
