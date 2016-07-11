fun bar() = 1
fun foo() {
    val x = bar().var<caret>
}
