@Target(AnnotationTarget.TYPE)
annotation class Foo

fun test(a: <expr>List<@Foo UnresolvedType></expr>) {}
