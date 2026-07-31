// The default value of a value parameter is stubbed as long as the expression itself is stub-based
package test

const val constant: Int = 1

fun withDefaults(
    int: Int = 2,
    string: String = "s",
    negative: Int = -3,
    reference: Int = constant,
) {
}

annotation class WithArrayDefaults(
    val empty: IntArray = [],
    val filled: IntArray = [4, 5],
)

class WithConstructorDefaults(val value: Int = 6, text: String = "t")
