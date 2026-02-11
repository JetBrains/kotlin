// IGNORE_FIR

context(Int, String)
fun foo(b: Boolean) {

}

fun usage() {
    <expr>::foo</expr>
}

// LANGUAGE: +ContextReceivers, -ContextParameters