// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

fun <T : Any> nullable(): T? = null

fun test() {
    val value = nullable<Int>() ?: nullable()
}