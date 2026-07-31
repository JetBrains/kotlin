// A call is stub-based only inside a value argument as well, so the whole call subtree is currently absent.
// Once calls are stubbed in these positions, their own arguments become stubbed too, recursively
package test

fun compute(): Int = 0

fun sum(left: Int, right: Int): Int = left + right

val call: Int = compute()

val callWithArguments: Int = sum(1, 2)

val callWithNamedArguments: Int = sum(left = 3, right = 4)

val nestedCall: Int = sum(compute(), sum(5, 6))

val qualifiedCall: String = 7.toString()

val parenthesizedCall: Int = (compute())

val doublyParenthesizedCall: Int = ((compute()))

val callWithParenthesizedArguments: Int = sum((11), (12))

fun expressionBodyCall(): Int = compute()

fun expressionBodyNestedCall(): Int = sum(compute(), 8)

fun expressionBodyParenthesizedCall(): Int = (compute())

val getterCall: Int
    get() = compute()

fun withCallDefault(value: Int = compute()) {
}

class WithConstructorCallDefault(val value: Int = sum(9, 10))
