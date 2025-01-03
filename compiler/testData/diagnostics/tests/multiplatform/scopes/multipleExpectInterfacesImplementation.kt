// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57833

// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ByteChannel<!> : ByteReadChannel, ByteWriteChannel

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ByteReadChannel<!> {
    val isClosedForWrite: Boolean

    fun f()
}

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ByteWriteChannel<!> {
    val isClosedForWrite: Boolean

    fun f()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: platform.kt

actual interface ByteReadChannel {
    actual val isClosedForWrite: Boolean

    actual fun f()
}

actual interface ByteWriteChannel {
    actual val isClosedForWrite: Boolean

    actual fun f()
}
