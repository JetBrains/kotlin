// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
fun box(): String {
    return <!UPPER_BOUND_VIOLATED!>someFunction<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><SomeEnum><!>()
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
