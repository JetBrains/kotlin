package test

inline fun test(a: String, b: String, c: () -> String): String {
    return a + b + c();
}