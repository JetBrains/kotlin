// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so JDK classes are not value classes there.
// See FirJvmPlatformValueClassDeterminer
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// WITH_STDLIB
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// JDK_KIND: FULL_JDK_21

import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional

fun test(date: LocalDate, optional: Optional<String>, zone: ZoneId, number: Number) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>date<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>optional<!>) {}
    // Value-based, but not a value class.
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>zone<!>) {}
    // A Kotlin class, which only maps to the Java value class `java.lang.Number`.
    synchronized(number) {}
}

value class ValueNumber(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Number<!>() {
    override fun toByte(): Byte = x.toByte()
    override fun toDouble(): Double = x.toDouble()
    override fun toFloat(): Float = x.toFloat()
    override fun toInt(): Int = x
    override fun toLong(): Long = x.toLong()
    override fun toShort(): Short = x.toShort()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral, override, primaryConstructor,
propertyDeclaration, value */
