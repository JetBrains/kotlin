@Target(AnnotationTarget.TYPE)
annotation class Anno(val message: String)

val nullablePropertyWithAnnotatedType: @Anno("outer") List<@Anno("middle") List<@Anno("inner") Int>>?
    get() = null

val proper<caret>tyToResolve: String
    get() = nullablePropertyWithAnnotatedType.let { " ($it)" }