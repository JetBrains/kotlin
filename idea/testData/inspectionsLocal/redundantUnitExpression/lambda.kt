// PROBLEM: none

fun <T> run(f: () -> T) = f()

fun foo(s: String) = s

fun test() {
    run {
        foo("Hello")
        <caret>Unit
    }
}