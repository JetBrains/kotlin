// FIR_IDENTICAL
// SKIP_TXT

class Out<out T : CharSequence?>(val t: T)

fun foo() {
    // We have two constraints here:
    // Nothing? <: T (from argument `null` type)
    // T <: CharSequence?
    // And we fix T to `Nothing?`, because it's still more preferrable than constraint from the upper bound
    val x1 = Out(null)
    bar(<!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Nothing?>")!>x1<!>)
}

fun bar(w: Out<String?>) {}
