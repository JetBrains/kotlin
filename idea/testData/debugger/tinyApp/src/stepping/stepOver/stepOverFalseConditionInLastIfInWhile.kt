package stepOverFalseConditionInLastIfInWhile

fun main(args: Array<String>) {
    var i = 0

    while (true) {
        if (i > 0) {
            break
        }
        i++

        //Breakpoint!
        if (testSome()) {
            some()
        }
    }
}

fun testSome(): Boolean {
    return false
}

fun some() {}