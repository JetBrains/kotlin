//KT-571 Type inference failed
fun <T, R> let(t : T, body : (T) -> R) = body(t)
private fun double(d : Int) : Int = let(d * 2) {it / 10 + it * 2 <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 10}
