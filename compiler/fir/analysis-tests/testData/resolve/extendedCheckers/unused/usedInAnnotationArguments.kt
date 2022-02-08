annotation class Ann(val value: Int)

fun foo(): Int {
    val x = 3
    @Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>) val y = 5
    return y
}
