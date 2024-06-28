// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

val String.plusK: String
    get() = this + "K"

fun box(): String =
        ("O"::plusK).getter.callBy(mapOf())
