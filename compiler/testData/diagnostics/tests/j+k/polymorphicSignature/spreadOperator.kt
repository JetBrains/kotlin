// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_21

import java.lang.invoke.MethodHandle
import java.lang.invoke.VarHandle

fun test(mh: MethodHandle, vh: VarHandle) {
    mh.invokeExact("1", "2")
    mh.invokeExact(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>emptyArray(), "X")
    mh.invokeExact(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>arrayOf("A", "B"), "C", <!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>arrayOf("D", "E"))
    mh.invoke(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>arrayOf("A"))
    vh.get()
    vh.get(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>emptyArray())
    vh.set(<!SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR!>*<!>emptyArray())
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, stringLiteral */
