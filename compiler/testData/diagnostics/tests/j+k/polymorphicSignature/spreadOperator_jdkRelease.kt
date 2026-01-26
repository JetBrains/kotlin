// RUN_PIPELINE_TILL: FRONTEND
// JDK_KIND: FULL_JDK_21
// JDK_RELEASE: 11

import java.lang.invoke.MethodHandle
import java.lang.invoke.VarHandle

fun test(mh: MethodHandle, vh: VarHandle) {
    mh.invokeExact("1", "2")
    mh.invokeExact(*emptyArray(), "X")
    mh.invokeExact(*arrayOf("A", "B"), "C", *arrayOf("D", "E"))
    mh.invoke(*arrayOf("A"))
    vh.get()
    vh.get(*emptyArray())
    vh.set(*emptyArray())
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, stringLiteral */
