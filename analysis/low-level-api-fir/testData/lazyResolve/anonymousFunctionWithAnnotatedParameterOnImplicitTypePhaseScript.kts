@Target(AnnotationTarget.TYPE)
annotation class Anno(val message: String)

val nullablePropertyWithAnnotatedType: @Anno("str") String? get() = null

val proper<caret>tyToResolve get() = nullablePropertyWithAnnotatedType?.let {" ($it)" } ?: ""