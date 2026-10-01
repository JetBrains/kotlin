// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
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

fun write(value: Any): ByteArray =
    ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(value) } }.toByteArray()

fun read(bytes: ByteArray): Any = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

fun writeFails(value: Any): Boolean = try {
    write(value)
    false
} catch (e: InvalidClassException) {
    e.message == "${value::class.java.name}; cannot serialize due to final value class or strictly-initialized instance fields"
}

fun box(): String {
    if (!writeFails(Full(1, "a"))) return "Fail: Full"
    if (!writeFails(Inline("b"))) return "Fail: Inline"
    val read = read(write(WithWriteReplace(2)))
    if (read != WithWriteReplace(2)) return "Fail: WithWriteReplace $read"
    return "OK"
}
