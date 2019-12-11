
inline fun myRun(b: () -> Unit) = b()

fun foo() {
    var a: Int
    return

    myRun {
        return
    }
}
