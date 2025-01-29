@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(parameter: @Anno("1" + "2") String)
fun fo<caret>o() = parameter
