// !CALL: foo

fun <T> foo(f: () -> T) {}

fun test() {
    foo { b }
}