// LANGUAGE: +DataObjects
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

import java.io.*

data object NonSerializableDataObject

data object SerializableDataObject: Serializable

fun box(): String {
    ByteArrayOutputStream().use { baos ->
        ObjectOutputStream(baos).use { oos -> oos.writeObject(SerializableDataObject) }
        ByteArrayInputStream(baos.toByteArray()).use { bais -> assert(java.io.ObjectInputStream(bais).readObject() === SerializableDataObject) }
    }
    return "OK"
}