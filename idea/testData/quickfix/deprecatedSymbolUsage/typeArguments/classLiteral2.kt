// "Replace with 'Int::class.java'" "true"
// WITH_RUNTIME

@Deprecated("Use class literal", ReplaceWith("T::class.java"))
fun <T> foo() {
}

val x = <caret>foo<Int>()