// TARGET_BACKEND: JVM
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-63864
inline fun ok(): String {
    return foo(1, 1.0, 1.0f, 1L, "O", C(if (bar()) return "zap" else "K"))
}

fun box(): String {
    val ok = ok()
    if (ok != "OK") return "Fail: $ok"

    val r = log.toString()
    if (r != "bar;<clinit>;<init>;foo;") return "Fail: '$r'"

    return "OK"
}

// FILE: C.kt
class C(val str: String) {
    init {
        log.append("<init>;")
    }

    companion object {
        init {
            log.append("<clinit>;")
        }
    }
}

// FILE: util.kt
fun foo(x: Int, a: Double, b: Float, y: Long, z: String, c: C) =
        logged("foo;", z + c.str)

fun bar() = logged("bar;", false)

val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}
