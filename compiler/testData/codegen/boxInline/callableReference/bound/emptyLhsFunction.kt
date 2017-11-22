class X {
    fun x(): String {
        return foo("O", "K", ::y)
    }

    fun y(a: String, b: String): String = a + b
}

inline fun foo(a: String, b: String, f: (String, String) -> String): String {
    return f(a, b)
}

fun box(): String {
    return X().x()
}
