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
    System.identityHashCode(p1)
    System.identityHashCode(p2)
    System.identityHashCode(p3)

    WeakReference(p1)
    WeakReference(p2)
    WeakReference(p3)

    SoftReference(p1)
    SoftReference(p2)
    SoftReference(p3)

    val refQueue = ReferenceQueue<Any?>()
    PhantomReference(p1, refQueue)
    PhantomReference(p2, refQueue)
    PhantomReference(p3, refQueue)

    val cleaner = Cleaner.create()
    cleaner.register(p1) {}
    cleaner.register(p2) {}
    cleaner.register(p3) {}

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