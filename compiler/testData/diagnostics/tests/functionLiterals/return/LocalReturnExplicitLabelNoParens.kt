fun test2() {
    run @f{return@f 1}
}

fun run<T>(f: () -> T): T { return f() }