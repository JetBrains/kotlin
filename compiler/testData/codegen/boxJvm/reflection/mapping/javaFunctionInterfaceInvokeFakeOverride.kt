// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: SingleInvokeImpl.java
import kotlin.jvm.functions.Function1;

public class SingleInvokeImpl implements Function1<String, Integer> {
    @Override
    public Integer invoke(String value) { return value.length(); }
}

// FILE: box.kt
// Tests that a Java class implementing a Kotlin Function1 has exactly one non-abstract
// 'invoke' in memberFunctions, and that its parameter has a name (not null).

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    val invokeFns = SingleInvokeImpl::class.memberFunctions.filter { it.name == "invoke" }

    // There should be exactly one concrete (non-abstract) invoke
    val concreteInvokes = invokeFns.filter { !it.isAbstract }
    assertEquals(1, concreteInvokes.size,
        "Expected exactly 1 concrete invoke, got: ${concreteInvokes.map { "${it.name} abstract=${it.isAbstract} params=${it.parameters.map { p -> p.name }}" }}")

    val invoke = concreteInvokes.single()
    // The VALUE parameter must have a non-null name
    val valueParam = invoke.valueParameters.singleOrNull()
        ?: return "Fail: invoke should have exactly 1 value parameter"

    assertNotNull(valueParam.name,
        "invoke value parameter name must not be null, got: ${valueParam.name}")

    return "OK"
}
