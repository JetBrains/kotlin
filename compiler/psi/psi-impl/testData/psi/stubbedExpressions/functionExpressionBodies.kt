// The expression body of a function is absent from the stub tree; only `hasNoExpressionBody` is recorded
package test

const val constant: Int = 1

fun int(): Int = 2

fun string(): String = "s"

fun negative(): Int = -3

fun reference(): Int = constant

fun qualified(): Int = test.constant

fun inferred() = 4

fun <T> generic(value: T): T = value

class WithMember {
    fun member(): Int = 5
}
