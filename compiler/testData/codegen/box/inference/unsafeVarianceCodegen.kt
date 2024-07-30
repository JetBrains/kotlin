class A<out K> {
    fun foo(x: @UnsafeVariance K): K = x
}

fun test(a: A<*>): Any? {
    return a.foo("OK")
}

fun box(): String {
    return test(A<String>()) as String
}