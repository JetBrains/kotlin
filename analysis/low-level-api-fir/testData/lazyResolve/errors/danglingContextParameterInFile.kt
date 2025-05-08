// IGNORE_FIR
// ^KT-76932
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(para<caret>meter1 : @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
