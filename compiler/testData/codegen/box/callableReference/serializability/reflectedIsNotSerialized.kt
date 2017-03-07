// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import java.io.*
import kotlin.test.*

fun bar() {}

fun box(): String {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(::bar)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    val o = ois.readObject()
    ois.close()

    // Test that we don't serialize the reflected view of the reference: it's not needed because it can be restored at runtime
    val field = kotlin.jvm.internal.CallableReference::class.java.getDeclaredField("reflected").apply { isAccessible = true }
    assertNull(field.get(o))

    return "OK"
}
