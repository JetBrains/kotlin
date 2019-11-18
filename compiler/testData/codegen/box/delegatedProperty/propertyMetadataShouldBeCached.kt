// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

import java.util.IdentityHashMap
import kotlin.reflect.KProperty

class A {
    var foo: Int by IntHandler

    companion object {
        var bar: Any? by AnyHandler
    }
}

val baz: String by StringHandler



val metadatas = IdentityHashMap<KProperty<*>, Unit>()

fun record(p: KProperty<*>) = metadatas.put(p, Unit)

object IntHandler {
    operator fun getValue(t: Any?, p: KProperty<*>): Int { record(p); return 42 }
    operator fun setValue(t: Any?, p: KProperty<*>, value: Int) { record(p) }
}

object AnyHandler {
    operator fun getValue(t: Any?, p: KProperty<*>): Any? { record(p); return 3.14 }
    operator fun setValue(t: Any?, p: KProperty<*>, value: Any?) { record(p) }
}

object StringHandler {
    operator fun getValue(t: Any?, p: KProperty<*>): String { record(p); return p.name }
    operator fun setValue(t: Any?, p: KProperty<*>, value: String) { record(p) }
}

fun box(): String {
    val a = A()
    a.foo = 42
    a.foo = a.foo + baz.length
    a.foo = 239
    A.bar = baz + a.foo
    baz + A.bar

    if (metadatas.keys.size != 3)
        return "Fail: only three instances of KProperty should have been created\n${metadatas.keys}"

    return "OK"
}
