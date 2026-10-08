// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.invoke.MethodHandles
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@JvmInline
value class Inline(val x: Int)

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

class Holder {
    @Volatile
    var any: Any? = null
}

fun fieldUpdater(updater: AtomicReferenceFieldUpdater<Holder, Any?>, inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String) {
    updater.compareAndSet(Holder(), inline, inline)
    updater.weakCompareAndSet(Holder(), int, int)
    updater.compareAndSet(Holder(), date, date)
    updater.compareAndSet(Holder(), wrapper, wrapper)
    updater.compareAndSet(Holder(), string, string)
}

fun varHandle(inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String) {
    val handle = MethodHandles.lookup().findVarHandle(Holder::class.java, "any", Any::class.java)
    handle.compareAndSet(Holder(), inline, inline)
    handle.weakCompareAndSet(Holder(), int, int)
    handle.compareAndExchange(Holder(), date, date)
    handle.compareAndSet(Holder(), wrapper, wrapper)
    handle.compareAndSet(Holder(), string, string)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, equalityExpression, flexibleType,
functionDeclaration, isExpression, javaFunction, localProperty, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, value */
