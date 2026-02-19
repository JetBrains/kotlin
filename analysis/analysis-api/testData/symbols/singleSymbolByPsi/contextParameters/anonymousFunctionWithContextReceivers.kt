// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

val p = context(String, List<@Anno("str") Int>) fun<caret>(i: Int) {
}
