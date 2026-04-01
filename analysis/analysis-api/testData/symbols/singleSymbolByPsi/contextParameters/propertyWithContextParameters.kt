
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
val f<caret>oo: Int get() = 0