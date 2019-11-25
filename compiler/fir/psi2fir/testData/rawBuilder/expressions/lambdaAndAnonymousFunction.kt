fun <T> run(block: () -> T): T = block()

fun test_1() {
    run { return@run }
    run { return }
}

fun test_2() {
    run(fun (): Int { return 1 })
}