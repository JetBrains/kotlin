// WITH_STDLIB

@Target(AnnotationTarget.FIELD)
annotation class Anno

class Test {
    val foo: String
        @Anno field = ""
}