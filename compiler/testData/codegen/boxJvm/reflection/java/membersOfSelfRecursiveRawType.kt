// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JBase.java
public class JBase<T extends JBase, F extends T> {
    public T field = null;
    public F method() { return null; }
}

// FILE: JChild.java
public class JChild extends JBase {}

// FILE: box.kt
import kotlin.test.assertEquals

class KtSubclass : JChild()

fun box(): String {
    assertEquals(
        "var KtSubclass.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>",
        KtSubclass::class.members.single { it.name == "field" }.toString(),
    )

    assertEquals(
        "fun KtSubclass.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>",
        KtSubclass::class.members.single { it.name == "method" }.toString(),
    )

    return "OK"
}
