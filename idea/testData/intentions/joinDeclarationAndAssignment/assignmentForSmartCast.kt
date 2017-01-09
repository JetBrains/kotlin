// IS_APPLICABLE: false
fun foo() {
    val x: Any<caret>
    x = "123"
    x.length // Smart cast before join, error after join
}