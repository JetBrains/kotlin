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

