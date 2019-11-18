// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// SKIP_JDK6
// FULL_JDK

package test

import java.util.function.*

interface Interface1<T> : () -> T, Supplier<T> {
    override fun invoke() = get()
}

class Impl : Interface1<String> {
    override fun get(): String = "OK"
}

fun box(): String {
    return Impl()()
}
