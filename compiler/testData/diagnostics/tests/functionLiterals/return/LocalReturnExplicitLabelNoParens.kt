fun test2() {
    val x = run f@{return@f 1}
    x: Int
}

fun run<T>(f: () -> T): T { return f() }