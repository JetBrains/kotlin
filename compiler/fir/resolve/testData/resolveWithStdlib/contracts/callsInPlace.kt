fun test() {
    val x: Int
    run {
        x = 1
    }
    x.inc()
}

fun test_2() {
    repeat(10) {
        "test_2"
    }
}

fun test_3() {
    repeat(action = { "test_3" }, times = 10)
}

fun test_4() {
    1.takeUnless {
        "test_4"
        it > 0
    }
}

fun test_5() {
    1.takeUnless(predicate = {
        "test_5"
        it > 0
    })
}

inline fun myRun(block1: () -> Unit, block2: () -> Unit) {
    block1()
    block2()
}

fun test_6() {
    myRun({ "test_6_1" }) { "test_6_2" }
}

fun test_7() {
    myRun(block2 = { "test_7_2" }, block1 = { "test_7_1" })
}

fun myDummyRun(block: () -> Unit) {
    block()
}

fun test_8() {
    myDummyRun { "test_8" }
}