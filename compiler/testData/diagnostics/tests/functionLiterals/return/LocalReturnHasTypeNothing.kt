fun test() {
    run1 @f{(): Int ->
        (return@f 1): Nothing
    }
}

fun run1<T>(f: () -> T): T { return f() }