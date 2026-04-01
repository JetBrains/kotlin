@Target(AnnotationTarget.TYPE)
annotation class Foo

@Target(AnnotationTarget.TYPE)
annotation class Bar

fun test(a: <expr>@Foo List<@Bar String></expr>) {}
