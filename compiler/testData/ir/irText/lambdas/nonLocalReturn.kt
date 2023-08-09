// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// KT-61141: In testLrmFoo1 and testLrmFoo2, `print (message: kotlin.Any?)` instead of `print (message: kotlin.Int)`
// IGNORE_BACKEND: NATIVE

fun test0() {
    run {
        return
    }
}

fun test1() {
    run {
        return@run
    }
}

fun test2() {
    run lambda@{
        return@lambda
    }
}

// TODO we don't see 'return@lambda' inside internal lambda when we analyze an external lambda,
// so type information from NI is actually incorrect, see KT-18392
fun test3() {
    run lambda@{
        run {
            return@lambda
        }
    }
}

fun testLrmFoo1(ints: List<Int>) {
    ints.forEach lit@ {
        if (it == 0) return@lit
        print(it)
    }
}

fun testLrmFoo2(ints: List<Int>) {
    ints.forEach {
        if (it == 0) return@forEach
        print(it)
    }
}
