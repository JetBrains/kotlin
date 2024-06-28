// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect interface SdkBufferedSink {
    public fun write(arg: String = "default")
}

abstract class AbstractBufferedSinkAdapter() : SdkBufferedSink {
    override fun write(arg: String) {
    }
}

expect class BufferedSinkAdapter() : SdkBufferedSink {
    override fun write(arg: String)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface SdkBufferedSink {
    public actual fun write(arg: String): Unit
}

actual class BufferedSinkAdapter actual constructor() : AbstractBufferedSinkAdapter(), SdkBufferedSink {
}
