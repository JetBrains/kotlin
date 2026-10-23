@Target(AnnotationTarget.TYPE)
annotation class Anno1

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs1(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno2

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs2(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno3

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs3(val x: String)

fun test(block: <expr> @Anno1 @AnnoWithArgs1("") ((@Anno2 @AnnoWithArgs2("") String) -> @Anno3 @AnnoWithArgs3("") Int?) </expr>) {}
