// FILE: 1.kt

inline fun foo(a: String, b: String, f: (String, String) -> String): String {
    return f(a, b)
}

// FILE: 2.kt

class X {
    fun x(): String {
        return foo("O", "K", ::y)
    }

    fun y(a: String, b: String): String = a + b
}

fun box(): String {
    return X().x()
}
