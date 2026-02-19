// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(String, List<@Anno("str") Int>)
class F<caret>oo