// FIR_IDENTICAL

fun println() {}

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
        100
        42
    }
    catch (e: Throwable) {
        println()
        101
        24
    }
    finally {
        println()
        102
        555
    }
}
