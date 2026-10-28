// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

class Holder {
    @Volatile
    var any: Any? = null
}

fun atomicReference(reference: AtomicReference<Any?>, date: LocalDate) {
    reference.compareAndSet(date, date)
}

fun atomicReferenceOfValueBasedClass(reference: AtomicReference<LocalDate>, date: LocalDate) {
    reference.compareAndSet(date, date)
}

fun fieldUpdater(updater: AtomicReferenceFieldUpdater<Holder, Any?>, date: LocalDate) {
    updater.compareAndSet(Holder(), <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>date<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>date<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, propertyDeclaration */
