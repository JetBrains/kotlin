fun box(): String {
    return <!TYPE_MISMATCH!>someFunction<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><SomeEnum><!>()
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
