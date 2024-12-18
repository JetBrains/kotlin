@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(par<caret>ameter: @Anno("1"+"2") String)
val f<caret>oo: Int get() = 0