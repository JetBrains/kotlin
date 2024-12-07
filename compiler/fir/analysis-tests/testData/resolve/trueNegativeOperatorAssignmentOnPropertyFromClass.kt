// RUN_PIPELINE_TILL: BACKEND
// WITH_EXPERIMENTAL_CHECKERS
// WITH_EXTRA_CHECKERS

class Test(number: Int = 10) {
    var number: Int = number
        set(value) { /* println(value) */ }
}

var index = 10

val test: Test
    get() = Test(index).also { index++ }

fun main() {
    // This results in printing 111
     test.number = test.number + 100
    // But this should print 110
    // test.number += 100
}
