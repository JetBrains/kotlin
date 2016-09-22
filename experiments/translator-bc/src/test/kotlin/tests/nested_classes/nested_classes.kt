
class Outer() {

    class Nested(var i: Int) {

        fun test(): Int {
            this.i = 5
            return this.i
        }
    }
}

fun nested_test_1(k: Int): Int {
    val j = Outer.Nested(k - 1)
    j.i = k

    return j.i
}

fun nested_test_2(k: Int): Int {
    val j = Outer.Nested(k - 1)

    return j.test()
}