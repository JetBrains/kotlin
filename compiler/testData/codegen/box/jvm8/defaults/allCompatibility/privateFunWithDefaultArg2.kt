// CHECK_BYTECODE_LISTING
// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_STDLIB

interface Foo {

    fun bar(): String {
        return foo()

    }

    private fun foo(s: String = "OK"): String {
        return s
    }
}

fun box(): String {
    return object : Foo {}.bar()
}