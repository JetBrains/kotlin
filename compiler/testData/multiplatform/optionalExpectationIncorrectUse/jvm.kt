fun useInReturnType(): A? = null

annotation class AnotherAnnotation(val a: A)

@AnotherAnnotation(A())
fun useInAnotherAnnotation() {}
