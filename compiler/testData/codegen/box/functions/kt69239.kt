fun <T> foo(i: Any = 42) = i as? T

fun box(): String {
    foo<String>()
    return "OK"
}