// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun test(x: () -> Unit, y: () -> Unit) {

}

fun main() {
    <!INAPPLICABLE_CANDIDATE!>test<!> {
        1
    } {
        2
    }
}