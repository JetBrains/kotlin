package a

open class A {
    protected fun protectedFun(): String = "OK"
}

inline fun foo(crossinline bar: () -> String) = object {
    fun baz() = bar()
}.baz()

class BSamePackage: A() {
    fun test(): String = foo {
        protectedFun()
    }
}

// 0 INVOKESTATIC a/BSamePackage.access
