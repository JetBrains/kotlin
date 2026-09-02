@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

fun test(block: <expr>context (@Anno @AnnoWithArgs("") String) (String) -> Int?</expr>) {}
