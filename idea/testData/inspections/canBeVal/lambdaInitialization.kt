fun run(f: () -> Unit) = f()

fun foo() {
    var a: Int

    run {
        a = 20
    }
}