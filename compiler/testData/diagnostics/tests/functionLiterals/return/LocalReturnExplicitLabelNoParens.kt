// !CHECK_TYPE

fun test2() {
    val x = run f@{return@f 1}
    checkSubtype<Int>(x)
}

fun run<T>(f: () -> T): T { return f() }