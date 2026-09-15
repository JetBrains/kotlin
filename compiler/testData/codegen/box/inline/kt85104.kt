// MODULE: a
// FILE: A.kt

inline fun foo(arg: String = TODO()) = arg

// MODULE: b(a)
// FILE: test.kt

fun box(): String {
    try {
        foo()
        return "Fail: expected NotImplementedError"
    } catch (e: NotImplementedError) { }
    return "OK"
}
