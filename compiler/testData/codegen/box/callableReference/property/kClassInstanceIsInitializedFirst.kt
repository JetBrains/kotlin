// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty1

class A {
    companion object {
        val ref: KProperty1<A, String> = A::foo
    }

    val foo: String = "OK"
}

fun box(): String {
    return A.ref.get(A())
}
