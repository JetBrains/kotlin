// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so no class is known to be a value object at run time there.
// See isValueObjectAtRuntime
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21

// FILE: JavaValue.java
public value class JavaValue {
    public final int x;

    public JavaValue(int x) {
        this.x = x;
    }
}

// FILE: test.kt
import java.lang.ref.Cleaner
import java.lang.ref.PhantomReference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.ZoneId
import java.util.IdentityHashMap
import java.util.WeakHashMap

@JvmInline
value class Inline(val x: Int)

value class Full(val x: Int, val y: Int)

fun test(inline: Inline, full: Full, java: JavaValue, date: LocalDate, int: Int, zone: ZoneId) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>full<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java<!>)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>date<!>, null)
    Cleaner.create().register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>) {}
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Full<!>, Int>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate<!>, Int>()
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>zone<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral,
nullableType, primaryConstructor, propertyDeclaration, samConversion, value */
