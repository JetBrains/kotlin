// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.assertEquals

annotation class Name(val value: String)

annotation class Anno(
    @get:Name("O") val o: String,
    @get:Name("K") val k: String
)

fun box(): String {
    val ms = Anno::class.java.declaredMethods

    return (ms.single { it.name == "o" }.annotations.single() as Name).value +
           (ms.single { it.name == "k" }.annotations.single() as Name).value
}
