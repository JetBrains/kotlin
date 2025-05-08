// IGNORE_FIR
// ^KT-76932
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

@Anno("str")
context(@Anno("param") para<caret>meter1 : @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
