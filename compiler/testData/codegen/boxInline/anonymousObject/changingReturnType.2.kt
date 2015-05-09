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