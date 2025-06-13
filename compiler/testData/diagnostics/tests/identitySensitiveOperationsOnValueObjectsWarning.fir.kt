// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

import java.lang.ref.*
import java.time.LocalDate
import java.util.*


@JvmInline
value class VcString(val s: String) : Runnable {
    override fun run() {}
}

fun testCallArguments(p1: Int, p2: LocalDate, p3: VcString, p4: java.lang.Character, p5: java.lang.Runtime.Version, p6: java.time.chrono.JapaneseDate, p7: java.lang.ProcessHandle) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p7<!>)

    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p7<!>)

    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p7<!>)

    val refQueue = ReferenceQueue<Any?>()
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p7<!>, refQueue)

    val cleaner = Cleaner.create()
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p7<!>) {}

    // Don't report if 2nd parameter
    cleaner.register(Any(), p3)
}

fun testNullable(p1: Int?, p2: LocalDate?, p3: VcString?) {
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>)
}

fun testTypeParameters() {
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Int<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>VcString<!>, Any>()

    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Int<!>, Any>()
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate<!>, Any>()
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>VcString<!>, Any>()

    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Int?<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate?<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>VcString?<!>, Any>()

    // Test inferred parameters from the expected type
    val t1: Map<Int, Any> = <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>WeakHashMap<!>()
    val t2: Map<Int, Any> = WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>_<!>, _>()
}

fun testFlexibleTypes() {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Integer.valueOf(1)<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, javaFunction, lambdaLiteral,
localProperty, nullableType, override, primaryConstructor, propertyDeclaration, samConversion, value */
