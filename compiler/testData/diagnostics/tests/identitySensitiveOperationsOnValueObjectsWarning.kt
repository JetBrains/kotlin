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

fun testCallArguments(p1: Int, p2: LocalDate, p3: VcString, p4: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Character<!>, p5: java.lang.Runtime.Version, p6: java.time.chrono.JapaneseDate, p7: java.lang.ProcessHandle) {
    System.identityHashCode(p1)
    System.identityHashCode(p2)
    System.identityHashCode(p3)
    System.identityHashCode(p4)
    System.identityHashCode(p5)
    System.identityHashCode(p6)
    System.identityHashCode(p7)

    WeakReference(p1)
    WeakReference(p2)
    WeakReference(p3)
    WeakReference(p4)
    WeakReference(p5)
    WeakReference(p6)
    WeakReference(p7)

    SoftReference(p1)
    SoftReference(p2)
    SoftReference(p3)
    SoftReference(p4)
    SoftReference(p5)
    SoftReference(p6)
    SoftReference(p7)

    val refQueue = ReferenceQueue<Any?>()
    PhantomReference(p1, refQueue)
    PhantomReference(p2, refQueue)
    PhantomReference(p3, refQueue)
    PhantomReference(p4, refQueue)
    PhantomReference(p5, refQueue)
    PhantomReference(p6, refQueue)
    PhantomReference(p7, refQueue)

    val cleaner = Cleaner.create()
    cleaner.register(p1) {}
    cleaner.register(p2) {}
    cleaner.register(p3) {}
    cleaner.register(p4) {}
    cleaner.register(p5) {}
    cleaner.register(p6) {}
    cleaner.register(p7) {}

    // Don't report if 2nd parameter
    cleaner.register(Any(), p3)
}

fun testNullable(p1: Int?, p2: LocalDate?, p3: VcString?) {
    WeakReference(p1)
    WeakReference(p2)
    WeakReference(p3)
}

fun testTypeParameters() {
    WeakHashMap<Int, Any>()
    WeakHashMap<LocalDate, Any>()
    WeakHashMap<VcString, Any>()

    IdentityHashMap<Int, Any>()
    IdentityHashMap<LocalDate, Any>()
    IdentityHashMap<VcString, Any>()

    WeakHashMap<Int?, Any>()
    WeakHashMap<LocalDate?, Any>()
    WeakHashMap<VcString?, Any>()

    // Test inferred parameters from the expected type
    val t1: Map<Int, Any> = WeakHashMap()
    val t2: Map<Int, Any> = WeakHashMap<_, _>()
}

fun testFlexibleTypes() {
    System.identityHashCode(Integer.valueOf(1))
}