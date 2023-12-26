// TARGET_BACKEND: JVM
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-63864

// FILE: test.kt
fun box(): String {
    Outer().Inner(
            logged("i;", 1.let { it }),
            logged("j;", 2.let { it })
    )

    val result = log.toString()
    if (result != "i;j;Foo.<clinit>;Foo.<init>;Inner.<init>;") return "Fail: '$result'"

    return "OK"
}

// FILE: util.kt
val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}

// FILE: Foo.kt
open class Foo {
    init {
        log.append("Foo.<init>;")
    }

    companion object {
        init {
            log.append("Foo.<clinit>;")
        }
    }
}

class Outer {
    inner class Inner(val x: Int, val y: Int) : Foo() {
        init {
            log.append("Inner.<init>;")
        }
    }
}
