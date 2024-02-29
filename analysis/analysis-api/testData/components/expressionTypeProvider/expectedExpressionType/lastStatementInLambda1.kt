// WITH_STDLIB
// IGNORE_FE10
fun <T> run(f: () -> T) = f()

fun test() {
    run { f<caret>oo }
}
