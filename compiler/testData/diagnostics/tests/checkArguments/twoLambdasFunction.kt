// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun test(x: () -> Unit, y: () -> Unit) {

}

fun main() {
    <!NO_VALUE_FOR_PARAMETER!>test<!> {
        1
    } <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{
        2
    }<!>
}