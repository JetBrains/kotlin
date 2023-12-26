// FULL_JDK
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

val z = ArrayList<String>()

inline fun a(body: () -> Unit) {
    body()
    z += "a"
}

inline fun b(body: () -> Unit) {
    z += "b"
    body()
    a { z += "from b" }
}

fun test() {
    b { z += "test" }
}

fun box(): String {
    test()

    if (z != listOf("b", "test", "from b", "a"))
        return z.toString()

    return "OK"
}

