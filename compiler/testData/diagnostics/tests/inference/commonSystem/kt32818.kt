// !DIAGNOSTICS: -UNUSED_VARIABLE

fun <T : Any> nullable(): T? = null

fun test() {
    val value = nullable<Int>() ?: <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}!>nullable()<!>
}
