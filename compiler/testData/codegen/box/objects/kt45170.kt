object A {
    var id = 0
        get() = field.also { field++ }
}

fun box(): String {
    if (A.id != 0) return "FAIL1"
    if (A.id != 1) return "FAIL2"
    return "OK"
}
