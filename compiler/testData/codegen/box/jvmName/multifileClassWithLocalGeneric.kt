// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

@file:JvmName("Test")
@file:JvmMultifileClass
package test

fun foo(): String = bar()
fun bar(): String {
    open class LocalGeneric<T>(val x: T)
    class Derived(x: String) : LocalGeneric<String>(x)
    fun <T> LocalGeneric<T>.extFun() = this
    fun <T> localFun(x: LocalGeneric<T>) = x
    class Local3 {
        fun <T> method(x: LocalGeneric<T>) = x.x
    }
    return Local3().method(localFun(Derived("OK")).extFun())
}

fun box(): String = foo()
