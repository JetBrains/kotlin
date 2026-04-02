// foo.Nested
package foo

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

@Target(AnnotationTarget.TYPE)
annotation class AnotherAnnotation(val k: KClass<*>)

class Nested {
    fun @MyAnno("outer") List<@MyAnno("middle") List<@AnotherAnnotation(Nested::class) String>>.function(param: @MyAnno("outer") List<@MyAnno("middle") List<@AnotherAnnotation(Nested::class) String>>): @MyAnno("outer") List<@MyAnno("middle") List<@AnotherAnnotation(Nested::class) String>>? = null

    var property: @MyAnno("outer") List<@MyAnno("middle") List<@AnotherAnnotation(Nested::class) String>>? = null
}
