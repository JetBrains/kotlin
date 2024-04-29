// ISSUE: KT-62550
// TARGET_BACKEND: JVM
// WITH_STDLIB
// DUMP_IR
// FIR_IDENTICAL
// LAMBDAS: CLASS

// FILE: Helper.java
// Using Java to avoid different IR text dumps for K1 and K2
public class Helper {
    public static String renderSuperInterface(Object value) {
        return value.getClass().getGenericInterfaces()[0].toString();
    }
}

// FILE: main.kt
import kotlin.test.assertEquals

fun <T : Any> renderSuperInterface(value: T): String = Helper.renderSuperInterface(value)

fun getSuperInterface(block: () -> Any?): String = renderSuperInterface(block)

fun <T> getSuperInterfaceGeneric(block: () -> T): String = renderSuperInterface(block)

fun foo() {}

fun bar() {}

fun <E> materialize(): E = TODO()

typealias MyUnit = Unit

fun myUnit(): MyUnit = Unit

val lambdaEmpty: () -> Any = {}

val lambdaUnit: () -> Any = { foo() }

val lambdaMyUnit: () -> Any = { myUnit() }

val lambdaExplicitUnit: () -> Any = l@ {
    return@l Unit
}

val lambdaExplicitUnitOrString: () -> Any = l@ {
    if ("0".hashCode() == 42) return@l Unit
    ""
}

val lambdaString: () -> Any = { "propertyAssignment" }

val lambdaInt: () -> Any = { 42 }

val lambdaNothing: () -> Any? = { null as Nothing? }

val lambdaTypeVariableConstructor: () -> Any? = l@ {
    if ("0".hashCode() == 42) return@l materialize()
    Unit
}

fun box(): String {
    // Test ContextDependent & LambdaResolution resolution mode
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", getSuperInterface {})
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", getSuperInterface { bar() })
    assertEquals(
        "kotlin.jvm.functions.Function0<java.lang.Object>",
        getSuperInterface {
            if ("0".hashCode() == 42) return@getSuperInterface Unit
            ""
        }
    )
    assertEquals(
        "kotlin.jvm.functions.Function0<java.lang.Object>",
        getSuperInterface {
            if ("0".hashCode() == 42) return@getSuperInterface materialize()
            Unit
        }
    )
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", getSuperInterface { return@getSuperInterface Unit })
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", getSuperInterface { myUnit() })
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", getSuperInterface { "functionArgument" })
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", getSuperInterface { 100 })
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", getSuperInterface { null })
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", getSuperInterface { null as Nothing? })
    assertEquals("interface kotlin.jvm.functions.Function0", getSuperInterfaceGeneric { null })

    // Test WithExpectedType resolution mode
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", renderSuperInterface(lambdaEmpty))
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", renderSuperInterface(lambdaUnit))
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", renderSuperInterface(lambdaMyUnit))
    assertEquals("kotlin.jvm.functions.Function0<kotlin.Unit>", renderSuperInterface(lambdaExplicitUnit))
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", renderSuperInterface(lambdaTypeVariableConstructor))
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Object>", renderSuperInterface(lambdaExplicitUnitOrString))
    assertEquals("kotlin.jvm.functions.Function0<java.lang.String>", renderSuperInterface(lambdaString))
    assertEquals("kotlin.jvm.functions.Function0<java.lang.Integer>", renderSuperInterface(lambdaInt))
    assertEquals("interface kotlin.jvm.functions.Function0", renderSuperInterface(lambdaNothing))
    return "OK"
}