fun test() {
    run(@f{return@f 1}): Int
}


fun test1() {
    run(@{return@ 1}): Int
}

fun run<T>(f: () -> T): T { return f() }