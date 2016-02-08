package test

inline fun foo(x: String, y: String) = x + y

class A {
    fun test(s: String) = s
}

inline fun processRecords(block: (String) -> String): String {
    return A().test(block(foo("O", foo("K", "1"))))
}
