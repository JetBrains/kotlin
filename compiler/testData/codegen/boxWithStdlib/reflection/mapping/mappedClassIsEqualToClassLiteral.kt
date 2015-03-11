import kotlin.reflect.jvm.*

class A

fun box(): String {
    val a1 = javaClass<A>().kotlin
    val a2 = A::class

    if (a1 != a2) return "Fail equals"
    if (a1.hashCode() != a2.hashCode()) return "Fail hashCode"
    if (a1.toString() != a2.toString()) return "Fail toString"

    return "OK"
}
