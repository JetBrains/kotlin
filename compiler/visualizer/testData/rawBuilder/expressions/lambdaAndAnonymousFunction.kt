// FIR_IGNORE
//                             T fun (() -> T).invoke(): T
//                             │ │
fun <T> run(block: () -> T): T = block()

fun test_1() {
//  fun <T> run<Unit>(() -> T): T
//  │
    run { return@run }
//  fun <T> run<???>(() -> T): T
//  │
    run { return }
}

fun test_2() {
//  fun <T> run<Int>(() -> T): T
//  │                        Int
//  │                        │
    run(fun (): Int { return 1 })
}
