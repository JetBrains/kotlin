// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

annotation class Ann(val value: String)

val noAnnotations: Int
    get() = 0

@Ann("OK")
val oneAnnotation: String
    get() = ""

fun box(): String {
    assertEquals(emptyList(), ::noAnnotations.annotations)

    return (::oneAnnotation.annotations.single() as Ann).value
}
