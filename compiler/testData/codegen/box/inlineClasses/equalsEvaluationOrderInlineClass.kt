// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// !LANGUAGE: +InlineClasses

inline class Inner(val x: Int)
inline class A(val x: Inner)

var i = 0

fun set1(): A {
    i = 1
    return A(Inner(0))
}

fun test1(n: Int): A {
    if (i != 1)
        throw IllegalStateException("Fail $n")
    i = 0
    return A(Inner(0))
}

fun set1Boxed(): Any? = set1()
fun test1Boxed(n: Int): Any? = test1(n)

fun box(): String {
    try {
        set1() == test1(1)
        set1Boxed() == test1(2)
        set1() == test1Boxed(3)
        set1Boxed() == test1Boxed(4)
    } catch (e: IllegalStateException) {
        return e.message ?: "Fail no message"
    }
    return "OK"
}
