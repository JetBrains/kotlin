// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

open class A

fun <J : A> foo(j: J): J = j
fun <T> foo(j: T): T = j