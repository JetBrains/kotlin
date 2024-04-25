// FIR_IDENTICAL
// LANGUAGE: +PolymorphicSignature -ProhibitSpreadOnSignaturePolymorphicCall
// FULL_JDK

import java.lang.invoke.MethodHandle

fun test(mh: MethodHandle) {
    mh.invokeExact("1", "2")
    mh.invokeExact(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_WARNING!>*<!>emptyArray(), "X")
    mh.invokeExact(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_WARNING!>*<!>arrayOf("A", "B"), "C", <!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_WARNING!>*<!>arrayOf("D", "E"))
    mh.invoke(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_WARNING!>*<!>arrayOf("A"))
}
