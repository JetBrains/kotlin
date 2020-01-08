// !LANGUAGE: +PolymorphicSignature -ProhibitSpreadOnSignaturePolymorphicCall
// FULL_JDK

import java.lang.invoke.MethodHandle

fun test(mh: MethodHandle) {
    mh.invokeExact("1", "2")
    mh.<!INAPPLICABLE_CANDIDATE!>invokeExact<!>(*emptyArray(), "X")
    mh.<!INAPPLICABLE_CANDIDATE!>invokeExact<!>(*arrayOf("A", "B"), "C", *arrayOf("D", "E"))
    mh.invoke(*arrayOf("A"))
}
