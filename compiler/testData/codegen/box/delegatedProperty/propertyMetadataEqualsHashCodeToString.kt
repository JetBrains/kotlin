import java.util.HashSet

class A {
    val foo: String by O
    val Int.foo: String by O

    fun foo42() = 42.foo
}

val foo: String by O
val Int.foo: String by O

object O {
    val metadatas = HashSet<PropertyMetadata>()

    fun getValue(t: Any?, p: PropertyMetadata): String {
        metadatas.add(p)
        return ""
    }
}

fun box(): String {
    A().foo
    A().foo42()
    foo
    42.foo

    if (O.metadatas.size != 1)
        return "Too many different PropertyMetadata instances: ${O.metadatas}"

    val m = O.metadatas.iterator().next()
    if (m.toString() != "PropertyMetadata(name=foo)")
        return "Wrong toString(): $m"

    return "OK"
}
