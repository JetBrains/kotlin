// "Add new line after annotations" "true"

@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

fun foo(y: IntArray) {
    // Currently it calls reformatting of base expression, but it seems to be a minor issue
    @Ann y [ 0 + 9 *   4]<caret> = y[y [1]]
}
