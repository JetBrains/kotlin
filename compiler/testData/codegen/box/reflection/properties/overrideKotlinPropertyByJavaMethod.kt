// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J implements K {
    private String foo;

    @Override
    public String getFoo() {
        return foo;
    }

    @Override
    public void setFoo(String s) {
        foo = s;
    }
}

// FILE: K.kt

import kotlin.test.assertEquals
import kotlin.reflect.KParameter

interface K {
    var foo: String
}

fun box(): String {
    val p = J::foo
    assertEquals("foo", p.name)

    if (p.parameters.size != 1) return "Should have only 1 parameter"
    if (p.parameters.single().kind != KParameter.Kind.INSTANCE) return "Should have an instance parameter"

    if (J::class.members.none { it == p }) return "No foo in members"

    val j = J()
    p.setter.call(j, "OK")
    return p.getter.call(j)
}
