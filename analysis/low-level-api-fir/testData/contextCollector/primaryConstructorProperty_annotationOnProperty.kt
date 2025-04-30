// WITH_STDLIB

@Target(AnnotationTarget.PROPERTY)
annotation class MyAnnotation(val name: String)

class Foo<TypeParameter>(
    val a: String = "foo",
    @MyAnnotation(<expr>CONSTANT_NAME</expr>) val b: Int = a.length,
    val c: Long = (a.length - 1).toLong()
) {
    class NestedClass
    
    companion object {
        const val CONSTANT_NAME: String = "NAME"
    }
}