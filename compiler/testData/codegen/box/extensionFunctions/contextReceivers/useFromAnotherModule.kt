// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

// MODULE: lib
// FILE: A.kt

package a

context(Int)
fun one(dummy: Any?) = this@Int

context(Int)
val two get() = this@Int

class Foo {
    context(Int)
    val three get() = this@Int

    context(Int)
    fun four(dummy: Any?) = this@Int
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return with(1) {
        if (a.one(null) + a.two + a.Foo().three + a.Foo().four(null) == 4) "OK" else "fail"
    }
}
