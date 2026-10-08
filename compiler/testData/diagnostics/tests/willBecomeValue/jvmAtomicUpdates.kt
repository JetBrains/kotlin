// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

class Holder {
    @Volatile
    var wrapper: Wrapper? = null
}

fun test(ref: AtomicReference<Wrapper>, array: AtomicReferenceArray<Wrapper>, w: Wrapper, v: Wrapper) {
    ref.updateAndGet { it }
    ref.getAndUpdate { it }
    ref.accumulateAndGet(w) { a, _ -> a }
    ref.getAndAccumulate(w) { a, _ -> a }
    array.updateAndGet(0) { it }
    array.getAndUpdate(0) { it }
    array.accumulateAndGet(0, w) { a, _ -> a }
    array.getAndAccumulate(0, w) { a, _ -> a }

    val updater = AtomicReferenceFieldUpdater.newUpdater(Holder::class.java, Wrapper::class.java, "wrapper")
    updater.compareAndSet(Holder(), w, v)
    updater.weakCompareAndSet(Holder(), w, v)

    val handle = MethodHandles.lookup().findVarHandle(Holder::class.java, "wrapper", Wrapper::class.java)
    handle.compareAndSet(Holder(), w, v)
    handle.weakCompareAndSet(Holder(), w, v)
    handle.compareAndExchange(Holder(), w, v)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, equalityExpression, flexibleType,
functionDeclaration, integerLiteral, isExpression, javaFunction, lambdaLiteral, localProperty, nullableType, operator,
override, primaryConstructor, propertyDeclaration, samConversion, smartcast, stringLiteral */
