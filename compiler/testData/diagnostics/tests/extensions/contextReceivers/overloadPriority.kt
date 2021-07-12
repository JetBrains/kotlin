class Context

context(Context)
fun f() {}

fun f() {}

fun test() {
    with(Context()) {
        f()
    }
}