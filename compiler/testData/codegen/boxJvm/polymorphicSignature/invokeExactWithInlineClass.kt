// LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
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
    return try {
        mh.invokeExact(Z("OK"))
        "FAIL"
    } catch (ignored: java.lang.invoke.WrongMethodTypeException) {
        "OK"
    }
}
