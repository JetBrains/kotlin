@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno1

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class AnnoWithArgs1(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno2

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs2(val x: String)

fun <@Anno1 @AnnoWithArgs1("") T> test(value: <expr>List<@Anno2 @AnnoWithArgs2("") T></expr>) {}
