// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

interface Source {
    fun read(sink: Buffer): String
}

expect class Buffer()

expect abstract class ForwardingSource: Source {
    override fun read(sink: Buffer): String
}

// MODULE: jvm()()(common)
// FILE: platfrom.kt

actual class Buffer actual constructor()

actual abstract class ForwardingSource : Source {
    actual override fun read(sink: Buffer): String = "OK"
}

fun box() : String{
    return (object : ForwardingSource() {}).read(Buffer())
}