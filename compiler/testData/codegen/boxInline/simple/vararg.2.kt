package test

inline fun doSmth(vararg a: String) : String {
    return a.foldRight("", {(a, b) -> a + b})
}