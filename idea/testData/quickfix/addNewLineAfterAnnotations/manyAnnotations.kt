// "Add new line after annotations" "true"

@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

@Target(AnnotationTarget.EXPRESSION)
annotation class Ann2(val x: String)

fun foo(y: Int) {
    var x = 1
    @Ann    @Ann2("") x<caret> += 2
}
