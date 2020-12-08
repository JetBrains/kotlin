// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW
// WITH_RUNTIME
// JDK_15
// IGNORE_DEXING

interface KI<T> {
    val x: String get() = ""
    val y: T
}

@JvmRecord
data class MyRec<R>(override val x: String, override val y: R) : KI<R>
