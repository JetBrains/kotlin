// JDK_RELEASE: 11
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class V(@JvmField var x: Int)

open class A(val value: String)
class C(value: String) : A(value)

fun box(): String {
    val varHandle = MethodHandles.lookup().findVarHandle(V::class.java, "x", Int::class.java)
    val v = V(0)
    varHandle.set(v, 42)
    val y = varHandle.get(v) as Int
    if (y != 42) return "Fail: $y"

    val ctor = MethodHandles.lookup().findConstructor(C::class.java, MethodType.methodType(Void.TYPE, arrayOf(String::class.java)))
    val o = ctor.invoke("O") as A
    val k = ctor.invokeExact("K") as C
    return o.value + k.value
}
