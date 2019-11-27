// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
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
