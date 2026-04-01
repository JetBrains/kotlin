
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(String, List<@Anno("str") Int>)
val f<caret>oo: Int get() = 0