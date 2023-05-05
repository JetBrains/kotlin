// IGNORE_BACKEND: JS
// WITH_STDLIB

annotation class Ann(val value: Int = Int.MIN_VALUE)

@Ann
class A

fun box(): String {
    val default = A::class.java.getAnnotation(Ann::class.java).value

    return if (default == Int.MIN_VALUE) "OK"
    else "FAIL"
}