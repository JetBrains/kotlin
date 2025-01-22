// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(parameter1: String, parameter2: List<Int>)
class F<caret>oo