// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException

interface I {
    fun get(): String
}

class C {
    fun run(i: I): String = i.get()
}

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "run",
        MethodType.methodType(String::class.java, I::class.java)
    )

    try {
        return mh.invokeExact(C(), object : I {
            override fun get(): String = "Fail"
        }) as String
    } catch (e: WrongMethodTypeException) {
        // OK
    }

    return mh.invoke(C(), object : I {
        override fun get(): String = "OK"
    }) as String
}
