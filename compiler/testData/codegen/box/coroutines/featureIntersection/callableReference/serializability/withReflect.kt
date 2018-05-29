// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT
// COMMON_COROUTINES_TEST
// WITH_RUNTIME

import java.io.*
import kotlin.test.*

class Foo {
    suspend fun method() {}
}

fun box(): String {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(Foo::method)
    oos.writeObject(::Foo)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    assertEquals(Foo::method, ois.readObject())
    assertEquals(::Foo, ois.readObject())
    ois.close()

    return "OK"
}
