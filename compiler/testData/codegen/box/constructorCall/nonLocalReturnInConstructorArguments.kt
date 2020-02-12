// !LANGUAGE: -NormalizeConstructorCalls -NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
fun box(): String {
    run L1@{
        var count = 0
        run {
            while (true) {
                Foo(
                        logged("i", if (count == 0) 1 else return@L1),
                        logged("j", 2)
                )
                count++
            }
        }
    }

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