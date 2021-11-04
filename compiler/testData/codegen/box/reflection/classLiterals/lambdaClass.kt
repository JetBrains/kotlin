// IGNORE_BACKEND: WASM
// KT-33992

class P<T>(val a: T, val b: T)

inline fun foo(x: () -> Any) = P(x(), x())

fun box(): String {
    val p1 = foo {
        class C
        C()
    }
    val p2 = foo {
        object {}
    }

    val x = p1.a
    val y = p1.b

    val a = p2.a
    val b = p2.b

    if (x::class != y::class) return "FAIL 1"
    if (a::class != b::class) return "FAIL 2"

    return "OK"
}

