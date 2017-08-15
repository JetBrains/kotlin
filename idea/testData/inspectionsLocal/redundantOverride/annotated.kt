// PROBLEM: none
@Target(AnnotationTarget.FUNCTION)
annotation class Annotation

open class Foo {
    open fun simple() {
    }
}

class Bar : Foo() {
    @Annotation override <caret>fun simple() {
        super.simple()
    }
}

