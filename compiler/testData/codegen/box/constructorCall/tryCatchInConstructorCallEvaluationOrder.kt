// !LANGUAGE: -NormalizeConstructorCalls
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
fun box(): String {
    Foo(
            logged("i", try { 1 } catch (e: Exception) { 42 }),
            logged("j", 2)
    )

    val result = log.toString()
    if (result != "<clinit>ij<init>") return "Fail: '$result'"

    return "OK"
}

// FILE: util.kt
val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}

// FILE: Foo.kt
class Foo(i: Int, j: Int) {
    init {
        log.append("<init>")
    }

    companion object {
        init {
            log.append("<clinit>")
        }
    }
}
