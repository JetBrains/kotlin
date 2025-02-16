// JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

interface Z<T> {
    fun test(p: T): T {
        return p
    }
}

open class ZImpl : Z<String>

// MODULE: main(lib)
// FILE: box.kt

open class ZImpl2 : Z<String>, ZImpl()

class ZImpl3 : ZImpl2() {

    override fun test(p: String): String {
        return super.test(p)
    }
}

fun box(): String {
    return ZImpl3().test("OK")
}
