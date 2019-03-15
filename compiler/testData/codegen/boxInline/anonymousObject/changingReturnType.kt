// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

open class Entity(val value: String)

public abstract class Task<T>() {
    abstract fun calc(): T
}

fun <Self : Entity> nullableTask(factory: () -> Task<Self>): Task<Self> {
    return factory()
}

inline fun<reified Self : Entity> Self.directed(): Task<Self> =
        nullableTask {
            object : Task<Self>() {
                override fun calc(): Self = this@directed
            }
        }

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
//KT-7490
import test.*

fun box(): String {
    return Entity("OK").directed().calc().value
}
