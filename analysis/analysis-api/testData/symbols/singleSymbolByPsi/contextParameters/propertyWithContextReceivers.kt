// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(String, List<@Anno("str") Int>)
val f<caret>oo: Int get() = 0