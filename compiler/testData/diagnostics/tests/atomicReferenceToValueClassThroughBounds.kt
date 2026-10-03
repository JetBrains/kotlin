// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK
// WITH_STDLIB

import java.util.concurrent.atomic.AtomicReference

@JvmInline
value class Box(val name: String)

fun <T : <!FINAL_UPPER_BOUND!>Box<!>> boxBound(ref: AtomicReference<T>, t: T) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)<!>
}

fun <T : Int?> intBound(ref: AtomicReference<T>, t: T) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)<!>
}

fun <T> intersection(ref: AtomicReference<T>, t: T) where T : Comparable<T>, T : Box? {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)<!>
}

fun <T : Any> noValueBound(ref: AtomicReference<T>, t: T) {
    ref.compareAndSet(t, t)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration,
typeConstraint, typeParameter, value */
