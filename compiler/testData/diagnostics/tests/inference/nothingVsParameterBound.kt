// FIR_IDENTICAL
// SKIP_TXT

class Out<out T : CharSequence?>(val t: T)

fun foo() {
    val x1 = Out(null)
    bar(x1)
}

fun bar(w: Out<String?>) {}
