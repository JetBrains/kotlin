fun test() {
    run(@f{<!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f 1<!>}): Int
}


fun test1() {
    run(@{<!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@ 1<!>}): Int
}

fun run<T>(f: () -> T): T { return f() }