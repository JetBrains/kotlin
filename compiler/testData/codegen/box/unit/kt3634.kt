val c = Unit.VALUE
val d = c

fun box(): String {
    c
    d
    return "OK"
}
