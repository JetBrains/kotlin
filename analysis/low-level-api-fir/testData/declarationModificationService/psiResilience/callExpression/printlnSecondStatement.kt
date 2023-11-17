// IGNORE_FIR
// Reason: KT-63560

fun foo(string: String) {
    val a = 7
    <expr>println("Hello world, $string!")</expr>
}
