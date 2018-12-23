// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

import java.util.concurrent.atomic.AtomicReference
import java.util.Arrays

fun box(): String {
    val a0 = AtomicReference(0.toUByte()).get().javaClass
    val a1 = AtomicReference(0u).get().javaClass

    val b = Arrays.asList(42u).first().javaClass

    if (a0.toString() != "class kotlin.UByte") return "Fail 1"
    if (a1.toString() != "class kotlin.UInt") return "Fail 2"
    if (b.toString() != "class kotlin.UInt") return "Fail 3"

    return "OK"
}