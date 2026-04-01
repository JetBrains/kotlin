@Target(AnnotationTarget.TYPE)
annotation class A

@Target(AnnotationTarget.TYPE)
annotation class B

@Target(AnnotationTarget.TYPE)
annotation class C

class Entity<X, Y, Z>

fun test(a: <expr>Entity<@A A, @B B, @C C></expr>) {}
