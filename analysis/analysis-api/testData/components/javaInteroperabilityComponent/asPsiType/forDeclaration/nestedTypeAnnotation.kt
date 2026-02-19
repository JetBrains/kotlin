package foo

@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

@Target(AnnotationTarget.TYPE)
annotation class AnotherAnnotation(val k: KClass<*>)

class Nested

fun f<caret>oo(): @MyAnno("outer") List<@MyAnno("middle") List<@AnotherAnnotation(Nested::class) String>> {

}
