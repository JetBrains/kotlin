// !LANGUAGE: +PolymorphicSignature
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_RUNTIME

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

    val r1 = mh.invokeExact(args)
    if (r1 != "aaa") return "Fail 1: $r1"

    // Spread operator is prohibited for arguments to signature-polymorphic calls
    // val r2 = mh.invokeExact(*args)
    // if (r2 != "aaa") return "Fail 2: $r2"

    val r3 = mh.invokeExact(arrayOf(args) as Array<*>)
    if (r3 !is Array<*> || !r3.contentEquals(args)) return "Fail 3: $r3"

    try {
        mh.invokeExact(arrayOf(args))
        return "Fail 4"
    } catch (e: WrongMethodTypeException) {
        // OK
    }

    val r5 = mh.invoke(args)
    if (r5 != "aaa") return "Fail 5: $r5"

    // val r6 = mh.invoke(*args)
    // if (r6 != "aaa") return "Fail 6: $r6"

    val r7 = mh.invoke(arrayOf(args) as Array<*>)
    if (r7 !is Array<*> || !r7.contentEquals(args)) return "Fail 7: $r7"

    val r8 = mh.invoke(arrayOf(args))
    if (r8 !is Array<*> || !r8.contentEquals(args)) return "Fail 8: $r8"

    // The next two calls check behavior in a statement context (where the call result is not used)

    try {
        mh.invokeExact(args)
        return "Fail 9"
    } catch (e: WrongMethodTypeException) {
        // OK
    }

    mh.invoke(args)

    return "OK"
}
