// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    bar(<caret>("" ?: return) in listOf(""))
}

fun bar(arg: Boolean) {}