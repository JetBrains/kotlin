// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.ref.PhantomReference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicStampedReference

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun atomics(stamped: AtomicStampedReference<Wrapper>, markable: AtomicMarkableReference<Wrapper>, w: Wrapper, v: Wrapper) {
    stamped.compareAndSet(w, v, 0, 1)
    stamped.weakCompareAndSet(w, v, 0, 1)
    stamped.attemptStamp(w, 1)
    markable.compareAndSet(w, v, false, true)
    markable.weakCompareAndSet(w, v, false, true)
    markable.attemptMark(w, true)
}

fun references(weak: WeakReference<Wrapper>, soft: SoftReference<Wrapper>, phantom: PhantomReference<Wrapper>, w: Wrapper) {
    weak.refersTo(w)
    soft.refersTo(w)
    phantom.refersTo(w)
    weak.refersTo(null)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
