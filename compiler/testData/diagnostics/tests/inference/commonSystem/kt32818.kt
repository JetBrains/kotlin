// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

fun <T : Any> nullable(): T? = null

fun test() {
    val value = nullable<Int>() ?: nullable()
}
