// PROBLEM: none
fun test(a: Any) {
    foo {
        val b = a as Boolean;<caret>
        !b
    }
}

fun foo(f: () -> Boolean) {}