// IS_APPLICABLE: true
fun bar(f: () -> Unit) {}
fun foo(a: Int, b: Int) = 2

fun test() {
    bar({
        <caret>val a = foo(1, 2)
    })
}