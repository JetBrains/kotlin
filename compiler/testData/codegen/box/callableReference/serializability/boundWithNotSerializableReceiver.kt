// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT
// FULL_JDK

import java.io.*
import kotlin.test.*

class Foo(val value: String)

fun box(): String {
    val oos = ObjectOutputStream(ByteArrayOutputStream())
    try {
        oos.writeObject(Foo("abacaba")::value)
        return "Fail: Foo is not Serializable and thus writeObject should have thrown an exception"
    }
    catch (e: NotSerializableException) {
        return "OK"
    }
}
