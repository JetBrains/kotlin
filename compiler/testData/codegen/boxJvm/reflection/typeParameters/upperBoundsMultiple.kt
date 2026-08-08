// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class MultiUpperBound<T>(val value: T) where T : Comparable<T>, T : java.io.Serializable

class SingleBound<T : Number>(val value: T)

class NoBound<T>(val value: T)

class SelfReferential<T : SelfReferential<T>>(val child: T? = null)

fun <T> multipleWhere(x: T): T where T : Comparable<T>, T : Cloneable = x

fun box(): String {
    // Class with multiple where clauses
    val multiParams = MultiUpperBound::class.typeParameters
    assertEquals(1, multiParams.size)
    val t = multiParams.single()
    assertEquals("T", t.name)
    assertEquals(KVariance.INVARIANT, t.variance)
    assertEquals(2, t.upperBounds.size)
    val boundNames = t.upperBounds.map { it.toString() }.sorted()
    assertTrue(boundNames.any { it.contains("Comparable") })
    assertTrue(boundNames.any { it.contains("Serializable") })

    // Class with single bound
    val singleParams = SingleBound::class.typeParameters
    assertEquals(1, singleParams.size)
    assertEquals(1, singleParams.single().upperBounds.size)
    assertTrue(singleParams.single().upperBounds.single().toString().contains("Number"))

    // Class with no explicit bound (upper bound is Any?)
    val noBoundParams = NoBound::class.typeParameters
    assertEquals(1, noBoundParams.size)
    assertEquals(1, noBoundParams.single().upperBounds.size)
    assertEquals("kotlin.Any?", noBoundParams.single().upperBounds.single().toString())

    // Self-referential bound
    val selfParams = SelfReferential::class.typeParameters
    assertEquals(1, selfParams.size)
    val selfT = selfParams.single()
    assertEquals(1, selfT.upperBounds.size)
    assertTrue(selfT.upperBounds.single().toString().contains("SelfReferential"))

    // Function with multiple where clauses
    val multiWhereFn = ::multipleWhere
    val fnTypeParams = multiWhereFn.typeParameters
    assertEquals(1, fnTypeParams.size)
    assertEquals(2, fnTypeParams.single().upperBounds.size)
    val fnBounds = fnTypeParams.single().upperBounds.map { it.toString() }.sorted()
    assertTrue(fnBounds.any { it.contains("Comparable") })
    assertTrue(fnBounds.any { it.contains("Cloneable") })

    // Declaration-site variance: invariant for class type params by default
    assertEquals(KVariance.INVARIANT, MultiUpperBound::class.typeParameters.single().variance)
    assertEquals(KVariance.INVARIANT, SingleBound::class.typeParameters.single().variance)

    return "OK"
}
