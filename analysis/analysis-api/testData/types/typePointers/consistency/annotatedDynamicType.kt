// TARGET_PLATFORM: JS

@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

fun test(value: <expr>@Anno @AnnoWithArgs("") dynamic</expr>) {}
