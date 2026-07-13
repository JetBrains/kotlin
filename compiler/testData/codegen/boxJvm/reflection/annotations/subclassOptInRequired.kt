// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.test.*

@RequiresOptIn("This is experimental")
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalFeature

@RequiresOptIn("This is another experimental API")
@Retention(AnnotationRetention.BINARY)
annotation class AnotherExperimentalApi

@SubclassOptInRequired(ExperimentalFeature::class)
abstract class ExperimentalBase(val value: Int) {
    abstract fun compute(): Int
    open fun describe(): String = "ExperimentalBase($value)"
}

@SubclassOptInRequired(AnotherExperimentalApi::class)
open class AnotherBase

// Class without @SubclassOptInRequired for contrast
abstract class RegularBase {
    abstract fun process(): String
}

fun box(): String {
    // @SubclassOptInRequired should be findable via findAnnotation
    val ann = ExperimentalBase::class.findAnnotation<SubclassOptInRequired>()
    assertNotNull(ann, "@SubclassOptInRequired not found on ExperimentalBase")
    assertEquals<Any>(ExperimentalFeature::class, ann.markerClass) // TODO: Interesting. Type inference fails here without <Any> but FE shows that it is redundant

    val ann2 = AnotherBase::class.findAnnotation<SubclassOptInRequired>()
    assertNotNull(ann2)
    assertEquals<Any>(AnotherExperimentalApi::class, ann2.markerClass)

    // hasAnnotation should return true
    assertTrue(ExperimentalBase::class.hasAnnotation<SubclassOptInRequired>())
    assertTrue(AnotherBase::class.hasAnnotation<SubclassOptInRequired>())

    // Regular class has no @SubclassOptInRequired
    assertNull(RegularBase::class.findAnnotation<SubclassOptInRequired>())
    assertFalse(RegularBase::class.hasAnnotation<SubclassOptInRequired>())

    // @SubclassOptInRequired is in the annotations list
    val annotations = ExperimentalBase::class.annotations
    val found = annotations.filterIsInstance<SubclassOptInRequired>()
    assertEquals(1, found.size)
    assertEquals<Any>(ExperimentalFeature::class, found.single().markerClass)

    // Class modifiers are unaffected by the annotation
    assertTrue(ExperimentalBase::class.isAbstract)
    assertFalse(ExperimentalBase::class.isSealed)
    assertFalse(ExperimentalBase::class.isFinal)

    // The annotation's own KClass has the expected structure
    assertTrue(SubclassOptInRequired::class.java.isAnnotation)

    return "OK"
}
