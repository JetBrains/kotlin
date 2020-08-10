import kotlin.contracts.*

infix fun <T> Any?.calledOn(value: Any?)

inline fun <T, R> with(value: T, block: (T) -> R): R {
    contract {
        block calledOn value
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(value)
}

fun test(s: Any?) {
    require(s is String?)
    with(s) {
        s?.length
        requireNotNull(it)
    }
    s.length // OK 
}