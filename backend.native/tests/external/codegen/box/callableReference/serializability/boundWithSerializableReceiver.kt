// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import java.io.*
import kotlin.test.*

data class Foo(val value: String) : Serializable

fun box(): String {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(Foo("abacaba")::value)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    assertEquals(Foo("abacaba")::value, ois.readObject())
    ois.close()

    return "OK"
}
