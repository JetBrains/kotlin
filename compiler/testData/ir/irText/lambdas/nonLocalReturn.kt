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