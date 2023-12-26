// TARGET_BACKEND: JVM
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-63864

// FILE: test.kt
fun box(): String {
    var count = 0
    while (true) {
        count++
        if (count > 1) break
        Foo(
                logged("i", if (count == 1) 1 else continue),
                logged("j", 2)
        )
    }

    val result = log.toString()
    if (result != "ij<clinit><init>") return "Fail: '$result'"

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
