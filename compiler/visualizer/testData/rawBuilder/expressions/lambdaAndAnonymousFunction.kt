//                             T fun (() -> T).invoke(): T
//                             │ │
fun <T> run(block: () -> T): T = block()

fun test_1() {
//  fun <T> run<Unit>(() -> Unit): Unit
//  │
    run { return@run }
//  fun <T> run<???>(() -> ???): ???
//  │
    run { return }
}

fun test_2() {
//  fun <T> run<Int>(() -> Int): Int
//  │                        Int
//  │                        │
    run(fun (): Int { return 1 })
}
