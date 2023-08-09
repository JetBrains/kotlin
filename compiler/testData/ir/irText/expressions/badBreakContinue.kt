// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// !IGNORE_ERRORS

// KT-61141: java.lang.RuntimeException: Loop not found for break expression: break
// IGNORE_BACKEND: NATIVE

fun test1() {
    break
    continue
}

fun test2() {
    L1@ while (true) {
        break@ERROR
        continue@ERROR
    }
}

fun test3() {
    L1@ while (true) {
        val lambda = {
            break@L1
            continue@L1
        }
    }
}

fun test4() {
    while (break) {}
    while (continue) {}
}
