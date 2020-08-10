import kotlin.contracts.*

infix fun <T> Any?.calledOn(value: Any?)

inline fun <T, R> test1(value: String, block: String.() -> R): R {
    <!WRONG_CALLED_ON_VALUE!>contract {
        block calledOn value
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>
    val a = ""

    value.block()
    block.invoke(value)
    block.<!WRONG_INVOCATION_VALUE!>invoke("value")<!>
    "value".<!WRONG_INVOCATION_VALUE!>block()<!>
    "123".<!WRONG_INVOCATION_VALUE!>block()<!>
    (value + "").<!WRONG_INVOCATION_VALUE!>block()<!>
    a.<!WRONG_INVOCATION_VALUE!>block()<!>
}