inline fun build(action: () -> Unit) {}

fun foo(x: Int) = build {
    if (x == 1) <!UNSUPPORTED!>[1]<!>
}
