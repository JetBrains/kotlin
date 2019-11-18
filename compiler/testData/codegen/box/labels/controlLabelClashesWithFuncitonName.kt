// IGNORE_BACKEND_FIR: JVM_IR
fun test1(): Boolean {
    test1@ for(i in 1..2) {
        continue@test1
        return false
    }

    return true
}

fun test2(): Boolean {
    test2@ while (true) {
        break@test2
    }

    return true
}

fun box(): String {
    if (!test1()) return "fail test1"
    if (!test2()) return "fail test2"
    return "OK"
}