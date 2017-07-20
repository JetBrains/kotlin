@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

fun foo(arg: String?) {
    (@Ann arg)<caret>!!
}