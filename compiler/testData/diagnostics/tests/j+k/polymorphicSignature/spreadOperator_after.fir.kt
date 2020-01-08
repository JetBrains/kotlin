// !LANGUAGE: +PolymorphicSignature +ProhibitSpreadOnSignaturePolymorphicCall
// FULL_JDK

import java.lang.invoke.MethodHandle

fun test(mh: MethodHandle) {
    mh.invokeExact("1", "2")
    mh.invokeExact(*emptyArray(), "X")
    mh.invokeExact(*arrayOf("A", "B"), "C", *arrayOf("D", "E"))
    mh.invoke(*arrayOf("A"))
}
