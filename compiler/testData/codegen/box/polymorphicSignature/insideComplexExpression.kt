// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object Counter {
    var result = 0
    var exception = false

    fun inc() {
        result++
        if (exception) throw AssertionError()
    }
}

val mh = MethodHandles.lookup().findVirtual(Counter::class.java, "inc", MethodType.methodType(Void.TYPE))

fun f(x: Int) {
    when (x) {
        1 -> try {
            throw Exception()
        } catch (e: Exception) {
            if (x > 42) {} else mh.invokeExact(Counter)
        }
        2 -> if (x < 42) {
            if (x < 4242) mh.invokeExact(Counter) else {}
        }
        3 -> try {
            try {
                mh.invokeExact(Counter)
            } finally {
                mh.invokeExact(Counter)
            }
        } catch (e: AssertionError) {
            Counter.exception = false
            mh.invokeExact(Counter)
        } finally {
            mh.invokeExact(Counter)
        }
    }
}

fun box(): String {
    f(1)
    f(2)

    Counter.exception = true
    f(3)

    return if (Counter.result == 6) "OK" else "Fail: ${Counter.result}"
}
