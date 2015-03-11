import kotlin.reflect.KMemberProperty

class A<T> {
    val result = "OK"
}

fun box(): String {
    val k = A::class.getProperties().single()
    k : KMemberProperty<A<*>, *>
    return k.get(A<String>()) as String
}
