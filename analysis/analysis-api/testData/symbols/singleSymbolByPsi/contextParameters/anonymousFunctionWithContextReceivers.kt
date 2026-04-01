// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

val p = context(String, List<@Anno("str") Int>) fun<caret>(i: Int) {
}
