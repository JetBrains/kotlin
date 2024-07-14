package kotlin

import kotlin.*

public class NotImplementedError(message: String = "An operation is not implemented.") : Error(message)

public inline fun TODO(): Nothing = throw NotImplementedError()

public inline fun <R> run(block: () -> R): R {
    return block()
}

public inline fun <T, R> T.run(block: T.() -> R): R {
    return block()
}

public inline fun <T, R> via(supplier: T, block: (T) -> R): R {
    return block(supplier)
}

public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

public inline fun <T> T.apply(block: T.() -> Unit): T {
    block()
    return this
}

public inline fun <T> T.also(block: (T) -> Unit): T {
    block(this)
    return this
}

public inline fun <T, R> T.let(block: (T) -> R): R {
    return block(this)
}

public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    return if (predicate(this)) this else null
}

public inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T? {
    return if (!predicate(this)) this else null
}

public inline fun repeat(times: Int, action: (Int) -> Unit) {
    for (index in 0 until times) {
        action(index)
    }
}

public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())
