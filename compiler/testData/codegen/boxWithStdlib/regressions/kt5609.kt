// KT-5609: interfaces should not inherit from KObject, unless explicitly said so

trait A

trait B : kotlin.jvm.internal.KObject

annotation class C

class D

class E : kotlin.jvm.internal.KObject

fun box(): String {
    val a = javaClass<A>().getGenericInterfaces().toList()
    if (a.toString() != "[]") return "Fail A: $a"

    val b = javaClass<B>().getGenericInterfaces().toList()
    if (b.toString() != "[interface kotlin.jvm.internal.KObject]") return "Fail B: $b"

    val c = javaClass<C>().getGenericInterfaces().toList()
    if (c.toString() != "[interface java.lang.annotation.Annotation]") return "Fail C: $c"

    val d = javaClass<D>().getGenericInterfaces().toList()
    if (d.toString() != "[interface kotlin.jvm.internal.KObject]") return "Fail D: $d"

    val e = javaClass<E>().getGenericInterfaces().toList()
    if (e.toString() != "[interface kotlin.jvm.internal.KObject]") return "Fail E: $e"

    return "OK"
}
