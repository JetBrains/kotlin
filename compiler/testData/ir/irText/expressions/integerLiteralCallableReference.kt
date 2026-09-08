// ISSUE: KT-88934
// DUMP_IR_DIFFERENCE: WASM_JS
fun test() {
    val x = (1+1)::unaryMinus
}
