@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(par<caret>ameter: @Anno("1"+"2") String)
fun foo() {}