
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(String, List<@Anno("str") Int>)
class F<caret>oo