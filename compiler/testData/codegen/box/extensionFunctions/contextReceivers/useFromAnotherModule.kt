// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

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

context(Int)
class Bar {
    fun five() = this@Int
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return with(1) {
        if (a.one(null) + a.two + a.Foo().three + a.Foo().four(null) + a.Bar().five() == 5) "OK" else "fail"
    }
}
