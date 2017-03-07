// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import java.io.*
import kotlin.test.*

class Foo(val prop: String) {
    fun method() {}
}

fun box(): String {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(Foo::prop)
    oos.writeObject(Foo::method)
    oos.writeObject(::Foo)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    assertEquals(Foo::prop, ois.readObject())
    assertEquals(Foo::method, ois.readObject())
    assertEquals(::Foo, ois.readObject())
    ois.close()

    return "OK"
}
