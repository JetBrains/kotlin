// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// WITH_STDLIB

import java.util.concurrent.atomic.AtomicReference

@JvmInline
value class Box(val name: String)

fun main() {
    val test = Box("Test")
    val rest = Box("Rest")
    val box = AtomicReference(test)

    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>box.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>test<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>rest<!>)<!>
    println(box.get())
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral, value */
