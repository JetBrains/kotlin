// WITH_RUNTIME

const val z = "OK"

annotation class A(val value: String = z)

@A
class Test

fun box(): String {
    return Test::class.java.getAnnotation(A::class.java).value
}
