@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

fun String.ba<caret>r() = foo()

context(parameter: @Anno("1" + "2") String)
fun foo() = parameter
