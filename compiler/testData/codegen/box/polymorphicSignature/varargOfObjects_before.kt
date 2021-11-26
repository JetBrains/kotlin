// !LANGUAGE: -PolymorphicSignature
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException

fun foo(vararg args: Any?): Any? = args[0]

fun box(): String {
    val mh = MethodHandles.lookup().findStatic(
        object {}::class.java.enclosingClass, "foo",
        MethodType.methodType(Any::class.java, Array<Any>::class.java)
    )

    val args = arrayOf("aaa", 1)

    // Note that before PolymorphicSignature was supported in the compiler, this call (and some subsequent calls) made little sense
    // because the vararg was always wrapped in another array. But it compiled and worked somehow, and this test checks its behavior.
    val r1 = mh.invokeExact(args)
    if (r1 !is Array<*> || !r1.contentEquals(args)) return "Fail 1: $r1"

    val r2 = mh.invokeExact(*args)
    if (r2 != "aaa") return "Fail 2: $r2"

    val r3 = mh.invokeExact(arrayOf(args) as Array<*>)
    if (r3 !is Array<*> || r3[0] !is Array<*> || !(r3[0] as Array<*>).contentEquals(args)) return "Fail 3: $r3"

    val r4 = mh.invokeExact(arrayOf(args))
    if (r4 !is Array<*> || r4[0] !is Array<*> || !(r4[0] as Array<*>).contentEquals(args)) return "Fail 4: $r4"

    val r5 = mh.invoke(args)
    if (r5 !is Array<*> || !r5.contentEquals(args)) return "Fail 5: $r5"

    val r6 = mh.invoke(*args)
    if (r6 != "aaa") return "Fail 6: $r6"

    val r7 = mh.invoke(arrayOf(args) as Array<*>)
    if (r7 !is Array<*> || r7[0] !is Array<*> || !(r7[0] as Array<*>).contentEquals(args)) return "Fail 7: $r7"

    val r8 = mh.invoke(arrayOf(args))
    if (r8 !is Array<*> || r8[0] !is Array<*> || !(r8[0] as Array<*>).contentEquals(args)) return "Fail 8: $r8"

    // The next two calls check behavior in a statement context (where the call result is not used)

    mh.invokeExact(args)

    mh.invoke(args)

    return "OK"
}
