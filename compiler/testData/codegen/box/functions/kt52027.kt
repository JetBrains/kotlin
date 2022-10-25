// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Foo {
    fun myFn() = "OK"
}

class Bar {
    context(Foo)
    inline fun ok() = myFn()
}

fun box(): String {
    with(Foo()) {
        return Bar().ok()
    }
}