// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo(bar: Int) {
    bar > 0.0 && 10.0 >= bar<caret>
}