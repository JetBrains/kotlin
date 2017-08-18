// IS_APPLICABLE: false

fun bar() {}

fun foo(f: Boolean) {
    <caret>if (f) bar()
}