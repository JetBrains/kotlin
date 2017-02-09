// IGNORE_BACKEND: JS

import java.io.*

fun box(): String {
    var o = ""
    var b = 0.toByte()
    var d = 0.0
    var f = 0.0f
    var i = 0
    var j = 0L
    var s = 0.toShort()
    var c = '0'
    var z = false

    val lambda = fun(): String {
        o = "OK"
        b++; d++; f++; i++; j++; s++; c++
        z = true
        return o
    }

    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(lambda)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    val result = ois.readObject() as () -> String
    ois.close()

    return result()
}
