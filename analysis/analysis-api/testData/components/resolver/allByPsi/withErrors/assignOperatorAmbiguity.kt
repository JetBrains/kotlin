class Test

operator fun Test.plus(i: Int): Test = this
operator fun Test.plusAssign(i: Int): Unit {}

operator fun Test.minus(i: Int): Test = this
operator fun Test.minusAssign(i: Int): Unit {}

operator fun Test.div(i: Int): Test = this
operator fun Test.divAssign(i: Int): Unit {}

operator fun Test.times(i: Int): Test = this
operator fun Test.timesAssign(i: Int): Unit {}

operator fun Test.rem(i: Int): Test = this
operator fun Test.remAssign(i: Int): Unit {}

fun test() {
    var foo = Test()
    foo += 1
    foo -= 1
    foo /= 1
    foo *= 1
    foo %= 1
}