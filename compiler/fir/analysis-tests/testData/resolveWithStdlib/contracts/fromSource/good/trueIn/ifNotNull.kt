import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> R.runIfNotNull(block: () -> R): R? {
    contract {
        (this@runIfNotNull != null) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this != null) block() else null
}

fun test1(s: String?) {
    s.runIfNotNull {
        s.length
    }
}