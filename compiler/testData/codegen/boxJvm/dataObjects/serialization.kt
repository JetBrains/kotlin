// LANGUAGE: +DataObjects
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

import java.io.*
import kotlin.test.*

data object NonSerializableDataObject

data object SerializableDataObject: Serializable

fun box(): String {
    ByteArrayOutputStream().use { baos ->
        ObjectOutputStream(baos).use { oos -> oos.writeObject(SerializableDataObject) }
        ByteArrayInputStream(baos.toByteArray()).use { bais ->
            val deseialized = ObjectInputStream(bais).readObject()
            assertEquals(SerializableDataObject, deseialized)
            assertNotSame(deseialized, SerializableDataObject)
        }
    }
    return "OK"
}