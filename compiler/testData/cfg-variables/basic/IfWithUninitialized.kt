fun foo() {
    val b: Boolean
    if (1 < 2) {
        use(b)
    }
    else {
        b = true
    }
}

fun use(vararg a: Any?) = a
