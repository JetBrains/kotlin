// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import kotlin.concurrent.thread

fun box(): String {
    val handle = MethodHandles.arrayElementVarHandle(ByteArray::class.java)
    val array = ByteArray(10)

    val index = 0

    // Check that we don't consider non-Object return type of a signature-polymorphic method to be polymorphic.
    handle.weakCompareAndSetPlain(array, index, 0.toByte(), 21.toByte()) as Comparable<*>

    val oldValue = 42.toByte()
    val newValue = (-74).toByte()

    thread {
        Thread.sleep(400L)

        handle.setVolatile(array, index, oldValue)
    }

    while (!handle.compareAndSet(array, index, oldValue, newValue)) {
        Thread.sleep(10L)
    }

    return if (handle.getVolatile(array, index) == newValue) "OK" else "Fail"
}
