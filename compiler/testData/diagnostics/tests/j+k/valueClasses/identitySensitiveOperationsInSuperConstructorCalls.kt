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

class FullRef(full: Full) : WeakReference<Full>(full)

class ZoneRef(zone: ZoneId) : WeakReference<ZoneId>(zone)

class SecondaryRef : WeakReference<Full> {
    constructor(full: Full) : super(full)
}

class FullMap : WeakHashMap<Full, Int>()

class ZoneMap : IdentityHashMap<ZoneId, Int>()

fun test(full: Full) = object : WeakReference<Full>(full) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, primaryConstructor,
propertyDeclaration, secondaryConstructor, value */
