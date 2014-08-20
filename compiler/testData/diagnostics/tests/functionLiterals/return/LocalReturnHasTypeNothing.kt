fun test() {
    run1 @f{(): Int ->
        (return@f 1)<!UNREACHABLE_CODE!>: Nothing<!>
    }
}

fun run1<T>(f: () -> T): T { return f() }