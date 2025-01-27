// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(para<caret>meter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
fun foo() {

}
