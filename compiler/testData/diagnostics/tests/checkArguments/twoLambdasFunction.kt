// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER

fun test(x: () -> Unit, y: () -> Unit) {

}

fun main() {
    <!NO_VALUE_FOR_PARAMETER!>test<!> {
        1
    } <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{
        2
    }<!>
}