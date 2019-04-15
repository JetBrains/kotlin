// "Replace with 'String::class'" "true"

@Deprecated("Use class literal", ReplaceWith("T::class"))
fun <T> foo() {
}

val x = <caret>foo<String>()