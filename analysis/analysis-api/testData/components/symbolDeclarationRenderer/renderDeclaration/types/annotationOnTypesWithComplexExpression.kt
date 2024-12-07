@Target(AnnotationTarget.TYPE)
annotation class A(val value: Int)

fun x(): @A(1 + 2) Int {
    TODO()
}