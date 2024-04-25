// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_STDLIB

interface Foo {

    fun bar(): String {
        return "OK"
    }
}

class FooClass : Foo {
    override fun bar(): String {
        return object {
            fun run()= super@FooClass.bar()
        }.run()
    }
}

fun box(): String {
    return FooClass().bar()
}