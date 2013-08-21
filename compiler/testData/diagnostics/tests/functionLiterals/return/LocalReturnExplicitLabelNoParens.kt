fun test2() {
    val x = run @f{<!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f 1<!>}
    x: Int
}

fun run<T>(f: () -> T): T { return f() }