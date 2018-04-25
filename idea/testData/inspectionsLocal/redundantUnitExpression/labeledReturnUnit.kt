fun foo(f: () -> Unit) {}

fun test() {
    foo {
        return@foo Unit<caret>
    }
}