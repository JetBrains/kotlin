@Target(AnnotationTarget.TYPE)
annotation class A

@Target(AnnotationTarget.TYPE)
annotation class B

@Target(AnnotationTarget.TYPE)
annotation class C

@Target(AnnotationTarget.TYPE)
annotation class D

@Target(AnnotationTarget.TYPE)
annotation class E

@Target(AnnotationTarget.TYPE)
annotation class F

fun test(a: <expr>List<Map<@A String, @B List<@C Map<String, @D Map<Int, @E List<@F Long>>>>></expr>) {}
