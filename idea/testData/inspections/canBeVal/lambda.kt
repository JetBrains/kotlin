fun run(f: () -> Unit) = f()

fun foo() {
    var a: Int
    a = 10

    run {
        a = 20
    }
}