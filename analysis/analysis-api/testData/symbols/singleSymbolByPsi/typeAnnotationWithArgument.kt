// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
@Target(AnnotationTarget.TYPE)
annotation class Anno5(val s: String)

fun f<caret>oo(): List<@Anno5("1") Int>? = null
