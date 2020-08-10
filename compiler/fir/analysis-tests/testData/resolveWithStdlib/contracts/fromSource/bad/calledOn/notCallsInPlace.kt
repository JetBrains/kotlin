import kotlin.contracts.*

infix fun <T> Any?.calledOn(value: Any?)

inline fun <T, R> test1(value: String, block: (String) -> R): R {
    <!NOT_DEFINITELY_INVOKED_IN_PLACE_LAMBDA!>contract {
        block calledOn value
    }<!>

    block(value)
}