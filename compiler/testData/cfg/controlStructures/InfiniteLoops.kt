fun unreachable() {}

fun a() {
    do {
    } while (true)
    unreachable()
}

fun b() {
    while (true) {
    }
    unreachable()
}