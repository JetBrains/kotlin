@Target(AnnotationTarget.TYPE)
annotation class A

@Target(AnnotationTarget.TYPE)
annotation class B

@Target(AnnotationTarget.TYPE)
annotation class C

fun test(a: <expr>List<Map<@A String, @B List<@C Int>>></expr>) {}
