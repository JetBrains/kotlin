// TARGET_BACKEND: JVM
// WITH_RUNTIME
// CONSTRUCTOR_CALL_NORMALIZATION_MODE: enable
// FILE: test.kt
fun box(): String {
    Foo(
            logged("i", listOf(1, 2, 3).map { it + 1 }.first()),
            logged("j",
                   Foo(
                           logged("k", listOf(1, 2, 3).map { it + 1 }.first()),
                           null
                   )
            )
    )

    val result = log.toString()
    if (result != "ik<clinit><init>j<init>") return "Fail: '$result'"

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
