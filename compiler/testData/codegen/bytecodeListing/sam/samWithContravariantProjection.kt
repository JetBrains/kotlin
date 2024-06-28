// FULL_JDK
// ISSUE: KT-62865
// FILE: test.kt
import java.util.function.Consumer

fun foo(x: Any) {}

class A {
    fun doOnSuccess(consumer: Consumer<in String>) {
    }
}

fun example() {
    val instance = A()
    instance.doOnSuccess(::foo)
}


