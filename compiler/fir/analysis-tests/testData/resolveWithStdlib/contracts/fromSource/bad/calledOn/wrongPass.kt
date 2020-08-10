import kotlin.contracts.*

infix fun <T> Any?.calledOn(value: Any?)

inline fun <R> test1(value: String, block: (String) -> R): R {
    <!WRONG_CALLED_ON_VALUE!>contract {
        block calledOn value
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>

    foo(value, block)
    bar("value", value, <!WRONG_INVOCATION_VALUE!>block<!>)

    block.foo(value)
    block.<!WRONG_INVOCATION_VALUE!>bar("123", value)<!>
}

inline fun <R> foo(value: String, block: (String) -> R): R {
    contract {
        block calledOn value
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(value)
}

inline fun <R> bar(value1: String, value2: String, block: (String) -> R): R {
    contract {
        block calledOn value1
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(value1)
}

inline fun <R> ((String) -> R).foo(value: String): R {
    contract {
        this@foo calledOn value
        callsInPlace(this@foo, InvocationKind.EXACTLY_ONCE)
    }
    this(value)
}

inline fun <T, R> ((String) -> R).bar(value1: String, value2: String): R {
    contract {
        this@bar calledOn value1
        callsInPlace(this@bar, InvocationKind.EXACTLY_ONCE)
    }
    this(value1)
}