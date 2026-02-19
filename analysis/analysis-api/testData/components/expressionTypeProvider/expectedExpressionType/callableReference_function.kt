// FULL_JDK
// WITH_STDLIB
import java.util.function.Consumer
import java.util.function.Function

class A<T>(val value: T) {
    val lock = Any()

    inline fun <U> withLock(block: (T) -> U): U = synchronized(lock) { block(value) }

    fun withLock(consumer: Consumer<in T>) {
        withLock(consumer::accept)
    }

    fun <U> withLock(function: Function<in T, out U>): U = withLock(function:<caret>:apply)
}