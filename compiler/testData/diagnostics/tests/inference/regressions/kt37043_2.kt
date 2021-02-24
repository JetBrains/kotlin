// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -USELESS_ELVIS

fun <T> first(x: Array<out T>): T = TODO()
fun <E> elvis(first: E?, second: E): E = TODO()

inline fun <reified M> materializeArray(): Array<M> = TODO()

fun nullableNumbers(): Array<Number?> = TODO()
fun notNullableNumbers(): Array<Number> = TODO()
fun nullableNumbersNullableArray(): Array<Number?>? = TODO()
fun notNullableNumbersNullableArray(): Array<Number>? = TODO()

fun main() {
    val number1 = first(elvis(notNullableNumbers(), materializeArray()))
    val number2 = first(elvis(notNullableNumbersNullableArray(), materializeArray()))
    val number3 = first(notNullableNumbers() ?: materializeArray())
    val number4 = first(notNullableNumbersNullableArray() ?: materializeArray())
    val nullableNumber1 = first(elvis(nullableNumbers(), materializeArray()))
    val nullableNumber2 = first(elvis(nullableNumbersNullableArray(), materializeArray()))
    val nullableNumber3 = first(nullableNumbers() ?: materializeArray())
    val nullableNumber4 = first(nullableNumbersNullableArray() ?: materializeArray())

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>number1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>number2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>number3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>number4<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumber1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumber2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumber3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumber4<!>
}
