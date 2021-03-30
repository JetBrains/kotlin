// "Change type to 'CharSequence'" "true"
interface A {
    val x: CharSequence
}

class B(override val x: Any<caret>) : A
/* FIR_COMPARISON */