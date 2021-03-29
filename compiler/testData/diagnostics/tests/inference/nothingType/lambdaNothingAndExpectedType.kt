// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <K> select2(x: K, y: K): K = TODO()
fun <K> select3(x: K, y: K, z: K): K = TODO()

fun test2(f: ((String) -> Int)?) {
    val a0: ((Int) -> Int)? = select2(<!TYPE_MISMATCH{OI}!>{ <!CANNOT_INFER_PARAMETER_TYPE{OI}!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}!>it<!> }<!>, null)
    val b0: ((Nothing) -> Unit)? = select2(<!TYPE_MISMATCH{OI}!>{ <!CANNOT_INFER_PARAMETER_TYPE{OI}!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}!>it<!> }<!>, null)

    select3({ it.length }, f, null)
}
