// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    operator fun <caret>iterator(): Iterator<Int> = throw Exception("")
}

fun test() {
    for (a in A(10)) {

    }
}