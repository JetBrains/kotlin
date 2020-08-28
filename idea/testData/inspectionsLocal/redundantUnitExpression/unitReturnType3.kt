// WITH_RUNTIME
// PROBLEM: none
// not yet supported
fun <T> doIt(p: () -> T): T = TODO()

fun x() = doIt<Unit> {
    4
    Unit<caret>
}
