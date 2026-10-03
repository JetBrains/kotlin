// COMPILATION_ERRORS
// WITH_STDLIB

@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

fun test(foo: <expr>List<@Anno @AnnoWithArgs("") Foo></expr>) {}
