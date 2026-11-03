// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses +ContextParameters +CompanionBlocks
// WITH_STDLIB
import java.io.*

value class Full(val x: Int, val y: String) : Serializable

@JvmInline
value class Inline(val s: String) : Serializable

value class WithWriteReplace(val x: Int) : Serializable {
    private fun writeReplace(): Any = Proxy(x)
}

class Proxy(val x: Int) : Serializable {
    private fun readResolve(): Any = WithWriteReplace(x)
}

abstract value class PrivateWriteReplaceBase : Serializable {
    private fun writeReplace(): Any = 0
}

value class InheritsPrivateWriteReplace(val x: Int) : PrivateWriteReplaceBase()

abstract value class InternalWriteReplaceBase : Serializable {
    internal fun writeReplace(): Any = 0
}

value class InheritsInternalWriteReplace(val x: Int) : InternalWriteReplaceBase()

value class WriteReplaceReturningString(val x: Int) : Serializable {
    private fun writeReplace(): String = ""
}

value class WriteReplaceWithContextParameter(val x: Int) : Serializable {
    context(c: Int)
    private fun writeReplace(): Any = c
}

value class CompanionBlockWriteReplace(val x: Int) : Serializable {
    companion {
        fun writeReplace(): Any = 0
    }
}

fun write(value: Any): ByteArray =
    ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(value) } }.toByteArray()

fun read(bytes: ByteArray): Any = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

fun writeFails(value: Any): Boolean = try {
    write(value)
    false
} catch (e: InvalidClassException) {
    e.classname == value::class.java.name
}

fun box(): String {
    if (!writeFails(Full(1, "a"))) return "Fail: Full"
    if (!writeFails(Inline("b"))) return "Fail: Inline"
    if (!writeFails(InheritsPrivateWriteReplace(3))) return "Fail: InheritsPrivateWriteReplace"
    if (!writeFails(InheritsInternalWriteReplace(4))) return "Fail: InheritsInternalWriteReplace"
    if (!writeFails(WriteReplaceReturningString(5))) return "Fail: WriteReplaceReturningString"
    if (!writeFails(WriteReplaceWithContextParameter(6))) return "Fail: WriteReplaceWithContextParameter"
    if (!writeFails(CompanionBlockWriteReplace(7))) return "Fail: CompanionBlockWriteReplace"
    val read = read(write(WithWriteReplace(2)))
    if (read != WithWriteReplace(2)) return "Fail: WithWriteReplace $read"
    return "OK"
}
