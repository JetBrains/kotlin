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

fun testCallArguments(p1: Int, p2: LocalDate, p3: VcString) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>)

    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>)

    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>)
    SoftReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>)

    val refQueue = ReferenceQueue<Any?>()
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>, refQueue)
    PhantomReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>, refQueue)

    val cleaner = Cleaner.create()
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>) {}
    cleaner.register(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>) {}

    // Don't report if 2nd parameter
    cleaner.register(Any(), p3)
}

fun testNullable(p1: Int?, p2: LocalDate?, p3: VcString?) {
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p1<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p2<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>p3<!>)
}

fun testTypeParameters() {
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>Int<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>LocalDate<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>VcString<!>, Any>()

    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>Int<!>, Any>()
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>LocalDate<!>, Any>()
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>VcString<!>, Any>()

    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>Int?<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>LocalDate?<!>, Any>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>VcString?<!>, Any>()

    // Test inferred parameters from the expected type
    val t1: Map<Int, Any> = <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>WeakHashMap<!>()
    val t2: Map<Int, Any> = WeakHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE_OBJECTS!>_<!>, _>()
}
