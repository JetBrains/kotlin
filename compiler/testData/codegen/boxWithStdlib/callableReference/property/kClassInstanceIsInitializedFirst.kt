import kotlin.reflect.KMemberProperty

class A {
    companion object {
        val ref: KMemberProperty<A, String> = A::foo
    }

    val foo: String = "OK"
}

fun box(): String {
    return A.ref.get(A())
}
