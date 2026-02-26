// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: J.java
import java.util.*;

@kotlin.jvm.PurelyImplements("kotlin.collections.MutableList")
public abstract class J<X> extends AbstractList<X> {}

// FILE: box.kt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("[java.util.AbstractList<X!>, kotlin.collections.MutableList<X>]", J::class.supertypes.toString())
    return "OK"
}
