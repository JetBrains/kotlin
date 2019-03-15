// PROBLEM: none

fun foo(f: () -> Any) {}

fun test() {
    foo {
        return@foo Unit<caret>
    }
}