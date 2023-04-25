// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun test1() {
    try {
        println()
    }
    catch (e: Throwable) {
        println()
    }
    finally {
        println()
    }
}

fun test2(): Int {
    return try {
        println()
        42
    }
    catch (e: Throwable) {
        println()
        24
    }
    finally {
        println()
        555
    }
}
