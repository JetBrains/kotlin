// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// SKIP_JDK6
// WITH_REFLECT

import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod

inline class Z(val s: String)

fun foo(z: Z): String = z.s

fun box(): String {
    val mh = MethodHandles.lookup().unreflect(::foo.javaMethod!!)

    // TODO: it's unclear whether this should throw or not, see KT-28214.
    val r1 = mh.invokeExact(Z("OK")) as String
    if (r1 != "OK") return "Fail r1: $r1"

    return mh.invokeExact("OK") as String
}
