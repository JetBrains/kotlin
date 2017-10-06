// "Add annotation target" "true"

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Foo

class Test {
    fun foo(): <caret>@Foo Int = 1
}