// IS_APPLICABLE: true
// WITH_RUNTIME
fun foo() {
    val x: String<caret>
    x = System.getProperty("")
}