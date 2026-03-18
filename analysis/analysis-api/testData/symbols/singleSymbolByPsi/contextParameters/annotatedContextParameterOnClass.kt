// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(para<caret>meter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
class Foo