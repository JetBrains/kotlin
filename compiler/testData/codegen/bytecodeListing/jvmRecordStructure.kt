// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW
// WITH_STDLIB
// JDK_KIND: FULL_JDK_17

// D8 does not yet desugar java records.
// IGNORE_DEXING

interface KI<T> {
    val x: String get() = ""
    val y: T
}

@JvmRecord
data class MyRec<R>(override val x: String, override val y: R) : KI<R>
