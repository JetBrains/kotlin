// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

val p = context(parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>) fun<caret>(i: Int) {
}
