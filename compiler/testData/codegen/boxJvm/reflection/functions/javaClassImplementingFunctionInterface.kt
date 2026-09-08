// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import kotlin.jvm.functions.Function1;

public class J implements Function1<String, Integer> {
    @Override
    public Integer invoke(String s) {
        return s.length();
    }
}

// FILE: JAbstract.java
import kotlin.jvm.functions.Function1;

public abstract class JAbstract implements Function1<String, Integer> {
}

// FILE: box.kt
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.test.*

fun box(): String {
    val invokes = J::class.memberFunctions.filter { it.name == "invoke" }
    assertEquals(1, invokes.size, "Expected a single invoke member, but was: $invokes")

    val invoke = invokes.single()
    assertFalse(invoke.isAbstract, "invoke must not be abstract: $invoke")

    val valueParameters = invoke.parameters.filter { it.kind == KParameter.Kind.VALUE }
    assertEquals(1, valueParameters.size, "Expected a single value parameter: $valueParameters")
    assertNotNull(valueParameters.single().name, "Value parameter name must not be null: $invoke")

    // An abstract Java class that does not implement `invoke` must still expose exactly one, abstract `invoke`.
    val abstractInvokes = JAbstract::class.memberFunctions.filter { it.name == "invoke" }
    assertEquals(1, abstractInvokes.size, "Expected a single invoke member, but was: $abstractInvokes")
    assertTrue(abstractInvokes.single().isAbstract, "invoke must be abstract: ${abstractInvokes.single()}")

    return "OK"
}
