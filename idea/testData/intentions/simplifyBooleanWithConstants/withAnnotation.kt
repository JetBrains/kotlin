var b = true

@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

fun foo() {
    if (@Ann <caret>b == true) {

    }
}