// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

infix fun Int.rem(other: Int) = 10
infix operator fun Int.minus(other: Int): Int = 20

const val a1 = (-5) rem 2
const val a2 = (-5).rem(2)

const val b1 = 5 minus 3
const val b2 = 5.minus(3)
