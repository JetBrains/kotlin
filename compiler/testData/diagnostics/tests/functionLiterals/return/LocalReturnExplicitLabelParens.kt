fun test() {
    run(@f{return@f 1})
}


fun test1() {
    run(@{return@ 1})
}

fun run<T>(f: () -> T): T { return f() }