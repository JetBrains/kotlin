fun unreachable() {}

fun a() {
    do {
    } while (true)
    <!UNREACHABLE_CODE!>unreachable()<!>
}

fun b() {
    while (true) {
    }
    <!UNREACHABLE_CODE!>unreachable()<!>
}

fun c() {
    do {} while (1 == 1)
}

fun d() {
    while (2 == 2) {}
}

fun use(arg: Any) = arg

fun f(cond: Boolean) {
    val bar: Any
    do {
        if (cond) {
            bar = "value"
            break
        }
    } while (true)
    use(bar) // should work

    val foo: Any
    while (true) {
        if (cond) {
            foo = "value"
            break
        }
    }
    use(foo) // should work
}

fun g(): Int {
    do {
        if (true) return 12
    } while (true)
} // should work

fun h(): Int {
    while (true) {
        if (true) return 12
    }
} // should work