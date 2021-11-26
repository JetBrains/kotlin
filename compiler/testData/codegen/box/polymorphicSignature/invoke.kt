// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

open class Base
class Derived : Base() {
    override fun toString() = "!"
}

class C {
    fun foo(s: String, d: Double?, x: Base): String = "$s$d$x"
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class IrrelevantAnnotation

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "foo",
        MethodType.methodType(String::class.java, String::class.java, Double::class.javaObjectType, Base::class.java)
    )

    val result1: String = mh.invoke(C(), "Hello", 0.01, Derived()) as String
    if (result1 != "Hello0.01!") return "Fail 1: $result1"

    // Check parenthesized/annotated expressions + invoke via "()"
    val result2 = (@IrrelevantAnnotation() ((mh(C(), (("Hello")), 0.01, Derived())))) as String
    if (result1 != result2) return "Fail 2: $result1 != $result2"

    // Check deep qualified expressions
    val o = object {
        val p = object {
            val handle = mh
        }
    }
    val result3 = (o.p).handle.invoke(C(), "Hello", 0.01, Derived()) as String
    if (result1 != result3) return "Fail 3: $result1 != $result3"

    // Check cast expression without assignment to a variable
    mh.invoke(C(), "", 0.01, Derived()) as String

    return "OK"
}
