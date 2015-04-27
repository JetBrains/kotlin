fun test() {
    val x = run(f@{return@f 1})
    x: Int
}


fun test1() {
    val x = run(l@{return@l 1})
    x: Int
}

fun run<T>(f: () -> T): T { return f() }