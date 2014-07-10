fun foo() {
    <caret>bar { }
}

fun bar(a: Any) = a
fun <T> bar(f: () -> T): T = f()
