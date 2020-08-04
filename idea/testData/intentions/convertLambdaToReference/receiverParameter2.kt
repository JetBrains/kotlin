// IS_APPLICABLE: false
class A(s: String) {
    fun bar(s: String) {}
}

fun foo(f: (String) -> Unit) {}

fun test() {
    foo { A(it).bar(it) <caret>}
}