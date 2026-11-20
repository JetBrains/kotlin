// LANGUAGE: +RichErrors
// DUMP_IR
fun foo(): NonError? = null
fun bar(): NonError = "OK"

fun box() = (foo() ?: bar()) as String
