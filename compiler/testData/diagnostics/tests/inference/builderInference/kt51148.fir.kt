fun box(): String {
    return <!INAPPLICABLE_CANDIDATE, UPPER_BOUND_VIOLATED!>someFunction<!><SomeEnum>()
}

interface SomeInterface <V> {

    val value: V

}

enum class SomeEnum {

    A, B, C

}

fun <V, T> someFunction(): String where T : Enum<T>, T : SomeInterface<V> {
    return "OK"
}