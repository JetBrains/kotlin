// !CALL: foo

fun <T> foo(f: (T) -> String) {}

fun test() {
    foo { (x: Int) -> "$x"}
}