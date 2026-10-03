// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

value class Full(val x: Int, val y: Int)

@JvmInline
value class Inline(val x: Int)

fun javaAtomics(full: AtomicReference<Full>, inline: AtomicReference<Inline>, int: AtomicReference<Int?>, array: AtomicReferenceArray<Full>) {
    full.compareAndSet(Full(1, 2), Full(3, 4))
    inline.compareAndSet(Inline(1), Inline(2))
    int.compareAndSet(1, 2)
    array.compareAndSet(0, Full(1, 2), Full(3, 4))
}

fun <T : <!FINAL_UPPER_BOUND!>Full<!>> bound(ref: AtomicReference<T>, t: T) {
    ref.compareAndSet(t, t)
}

fun kotlinAtomics(full: kotlin.concurrent.atomics.AtomicReference<Full>) {
    full.compareAndSet(Full(1, 2), Full(3, 4))
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter, value */
