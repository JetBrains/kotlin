import java.util.IdentityHashMap

class A {
    var foo: Int by IntHandler

    default object {
        var bar: Any? by AnyHandler
    }
}

val baz: String by StringHandler



val metadatas = IdentityHashMap<PropertyMetadata, Unit>()

fun record(p: PropertyMetadata) = metadatas.put(p, Unit)

object IntHandler {
    fun get(t: Any?, p: PropertyMetadata): Int { record(p); return 42 }
    fun set(t: Any?, p: PropertyMetadata, value: Int) { record(p) }
}

object AnyHandler {
    fun get(t: Any?, p: PropertyMetadata): Any? { record(p); return 3.14 }
    fun set(t: Any?, p: PropertyMetadata, value: Any?) { record(p) }
}

object StringHandler {
    fun get(t: Any?, p: PropertyMetadata): String { record(p); return p.name }
    fun set(t: Any?, p: PropertyMetadata, value: String) { record(p) }
}

fun box(): String {
    val a = A()
    a.foo = 42
    a.foo = a.foo + baz.length()
    a.foo = 239
    A.bar = baz + a.foo
    baz + A.bar

    if (metadatas.keySet().size() != 3)
        return "Fail: only three instances of PropertyMetadata should have been created\n${metadatas.keySet()}"

    return "OK"
}
