// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// RENDER_DIAGNOSTICS_FULL_TEXT
import java.util.IdentityHashMap
import java.util.WeakHashMap

value class Full(val x: Int, val y: Int)

fun test() {
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Full<!>, Int>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Int<!>, Int>()
}

class FullMap : WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Full<!>, Int>()

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, primaryConstructor,
propertyDeclaration, value */
