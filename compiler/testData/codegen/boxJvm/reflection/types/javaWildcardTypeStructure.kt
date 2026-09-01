// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: WildcardMethods.java
import java.util.*;

public class WildcardMethods {
    public static List<? extends Number>     boundedOut()    { return null; }
    public static List<? super Integer>      boundedIn()     { return null; }
    public static List<?>                    unbounded()     { return null; }
    public static Map<String, ? extends Number> mapBounded(){ return null; }
}

// FILE: box.kt
// Tests structural properties of reflected types for Java wildcard-typed methods:
// correct classifier, argument count, and variance projections.

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType
import kotlin.test.*

fun box(): String {
    val fns = WildcardMethods::class.staticFunctions.associateBy { it.name }

    // boundedOut(): List<? extends Number>
    // → classifier should be some List type, with 1 argument of OUT variance
    val boundedOut = fns["boundedOut"] ?: return "Fail: boundedOut not found"
    val boundedOutType = boundedOut.returnType
    val boundedOutClassifier = boundedOutType.classifier as? KClass<*>
    assertNotNull(boundedOutClassifier, "boundedOut return type must have a KClass classifier")
    assertTrue(boundedOutClassifier.qualifiedName?.contains("List") == true,
        "boundedOut return type classifier must be a List, got: ${boundedOutClassifier.qualifiedName}")
    assertEquals(1, boundedOutType.arguments.size,
        "boundedOut return type must have 1 type argument")
    val boundedOutArg = boundedOutType.arguments.single()
    assertEquals(KVariance.OUT, boundedOutArg.variance,
        "boundedOut type argument must be OUT (covariant)")
    val boundedOutArgType = boundedOutArg.type
    assertNotNull(boundedOutArgType, "boundedOut type argument must not be star")
    assertTrue(boundedOutArgType.toString().contains("Number"),
        "boundedOut type argument must be Number, got: $boundedOutArgType")

    // boundedIn(): List<? super Integer>
    // → 1 argument of IN variance
    val boundedIn = fns["boundedIn"] ?: return "Fail: boundedIn not found"
    val boundedInType = boundedIn.returnType
    assertEquals(1, boundedInType.arguments.size)
    val boundedInArg = boundedInType.arguments.single()
    assertEquals(KVariance.IN, boundedInArg.variance,
        "boundedIn type argument must be IN (contravariant)")
    assertTrue(boundedInArg.type?.toString()?.contains("Int") == true,
        "boundedIn type argument must contain Int, got: ${boundedInArg.type}")

    // unbounded(): List<?>
    // → 1 star-projection argument
    val unbounded = fns["unbounded"] ?: return "Fail: unbounded not found"
    val unboundedType = unbounded.returnType
    assertEquals(1, unboundedType.arguments.size)
    val unboundedArg = unboundedType.arguments.single()
    assertNull(unboundedArg.variance, "unbounded type argument must be a star projection (null variance)")
    assertNull(unboundedArg.type,     "unbounded type argument must be a star projection (null type)")

    // mapBounded(): Map<String, ? extends Number>
    // → 2 arguments; second is OUT
    val mapBounded = fns["mapBounded"] ?: return "Fail: mapBounded not found"
    val mapBoundedType = mapBounded.returnType
    assertEquals(2, mapBoundedType.arguments.size,
        "mapBounded return type must have 2 type arguments")
    val mapSecondArg = mapBoundedType.arguments[1]
    assertEquals(KVariance.OUT, mapSecondArg.variance,
        "mapBounded second type argument must be OUT")

    return "OK"
}
