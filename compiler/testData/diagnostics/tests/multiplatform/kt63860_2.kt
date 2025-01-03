// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SdkBufferedSink<!> {
    public fun write(arg: String = "default")
}

abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AbstractBufferedSinkAdapter<!>() {
    fun write(arg: String) {
    }
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>BufferedSinkAdapter<!>() : SdkBufferedSink {
    override fun write(arg: String)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface SdkBufferedSink {
    public actual fun write(arg: String): Unit
}

actual class BufferedSinkAdapter actual constructor() : AbstractBufferedSinkAdapter(), SdkBufferedSink {
}
