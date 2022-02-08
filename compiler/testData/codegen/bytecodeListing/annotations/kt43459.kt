annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno

class A {
    @Anno
    val @TypeAnno Int?.a: String
        get() = ""
}