// "Add parameter to function 'baz'" "true"
fun baz() {}

fun foo() {
    baz { i: Int -> i.toString() }<caret>
}