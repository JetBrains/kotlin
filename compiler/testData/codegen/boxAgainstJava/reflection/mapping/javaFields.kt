import javaFields as J

import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val i = J::i
    val s = J::s

    // Check that correct reflection objects are created
    assert(i.javaClass.getSimpleName() == "KMemberPropertyImpl", "Fail i class")
    assert(s.javaClass.getSimpleName() == "KMutableMemberPropertyImpl", "Fail s class")

    // Check that no Method objects are created for such properties
    assert(i.javaGetter == null, "Fail i getter")
    assert(s.javaGetter == null, "Fail s getter")
    assert(s.javaSetter == null, "Fail s setter")

    // Check that correct Field objects are created
    val ji = i.javaField!!
    val js = s.javaField!!
    assert(Modifier.isFinal(ji.getModifiers()), "Fail i final")
    assert(!Modifier.isFinal(js.getModifiers()), "Fail s final")

    // Check that those Field objects work as expected
    val a = J(42, "abc")
    assert(ji.get(a) == 42, "Fail ji get")
    assert(js.get(a) == "abc", "Fail js get")
    js.set(a, "def")
    assert(js.get(a) == "def", "Fail js set")
    assert(a.s == "def", "Fail js access")

    // Check that valid Kotlin reflection objects are created by those Field objects
    val ki = ji.kotlin as KMemberProperty<J, Int>
    val ks = js.kotlin as KMutableMemberProperty<J, String>
    assert(ki.get(a) == 42, "Fail ki get")
    assert(ks.get(a) == "def", "Fail ks get")
    ks.set(a, "ghi")
    assert(ks.get(a) == "ghi", "Fail ks set")

    return "OK"
}
