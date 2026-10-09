@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

fun foo(i: @Anno @AnnoWithArgs("") Int) {
    val iCopy = i
    <expr>iCopy</expr>
}
