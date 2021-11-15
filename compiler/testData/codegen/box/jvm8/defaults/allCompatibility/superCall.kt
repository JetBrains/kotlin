// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND_FIR: JVM_IR
// !JVM_DEFAULT_MODE: all-compatibility
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