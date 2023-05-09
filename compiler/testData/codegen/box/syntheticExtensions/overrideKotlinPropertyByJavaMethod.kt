// TARGET_BACKEND: JVM
// !LANGUAGE: +ReferencesToSyntheticJavaProperties

// WITH_STDLIB
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

interface K {
    var foo: String
}

fun box(): String {
    val p = J::foo
    assertEquals("foo", p.name)

    val j = J()
    p.set(j, "OK")
    return p.get(j)
}
