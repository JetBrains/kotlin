// TARGET_BACKEND: JVM
// WITH_RUNTIME
// CONSTRUCTOR_CALL_NORMALIZATION_MODE: disable
// FILE: test.kt
fun box(): String {
    Foo(
            logged("i", 1.let { it }),
            logged("j",
                   Foo(
                           logged("k", 2.let { it }),
                           null
                   )
            )
    )

    val result = log.toString()
    if (result != "<clinit>ik<init>j<init>") return "Fail: '$result'"

    return "OK"
}

// FILE: util.kt
val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}

// FILE: Foo.kt
class Foo(i: Int, j: Foo?) {
    init {
        log.append("<init>")
    }

    companion object {
        init {
            log.append("<clinit>")
        }
    }
}
