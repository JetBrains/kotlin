// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
import java.lang.ref.WeakReference
import java.time.ZoneId
import java.util.IdentityHashMap
import java.util.WeakHashMap

value class Full(val x: Int, val y: Int)

class FullRef(full: Full) : WeakReference<Full>(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>full<!>)

class ZoneRef(zone: ZoneId) : WeakReference<ZoneId>(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>zone<!>)

class SecondaryRef : WeakReference<Full> {
    constructor(full: Full) : super(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>full<!>)
}

class FullMap : WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>Full<!>, Int>()

class ZoneMap : IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ZoneId<!>, Int>()

fun test(full: Full) = object : WeakReference<Full>(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>full<!>) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, primaryConstructor,
propertyDeclaration, secondaryConstructor, value */
