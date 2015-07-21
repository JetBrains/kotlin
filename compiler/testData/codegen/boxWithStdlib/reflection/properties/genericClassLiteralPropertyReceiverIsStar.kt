import kotlin.reflect.KProperty1

class A<T> {
    val result = "OK"
}

fun box(): String {
    val k : KProperty1<A<*>, *> = A::class.properties.single()
    return k.get(A<String>()) as String
}
