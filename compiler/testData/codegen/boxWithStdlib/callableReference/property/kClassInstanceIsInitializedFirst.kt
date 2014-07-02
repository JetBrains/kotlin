import kotlin.reflect.KMemberProperty

class A {
    class object {
        val ref: KMemberProperty<A, String> = A::foo
    }

    val foo: String = "OK"
}

fun box(): String {
    return A.ref.get(A())
}
