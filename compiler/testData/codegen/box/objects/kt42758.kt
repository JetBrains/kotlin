// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

import java.io.*

object Thing : Serializable {
    private fun readResolve(): Any = Thing
}

private inline fun <reified T : Serializable> roundTrip(value: T): T {
    val outputStream = ByteArrayOutputStream()
    ObjectOutputStream(outputStream).use { it.writeObject(value) }
    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
    return ObjectInputStream(inputStream).use { it.readObject() } as T
}

fun box(): String {
    if (Thing !== roundTrip(Thing))
        throw Exception("Thing !== roundTrip(Thing)")

    return "OK"
}