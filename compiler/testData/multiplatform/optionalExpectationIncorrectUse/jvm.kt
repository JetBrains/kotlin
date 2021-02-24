fun useInReturnType(): A? = null

annotation class AnotherAnnotation(val a: A)

@AnotherAnnotation(A())
fun useInAnotherAnnotation() {}

actual class C {
    actual annotation class Nested
}
