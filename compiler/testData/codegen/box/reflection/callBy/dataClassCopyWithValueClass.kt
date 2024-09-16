// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.time.Duration

data class Test(val a: Duration, val b: Boolean)

fun box(): String {
    val test = Test(Duration.ZERO, false)
    val methodReference = test::copy
    val parameterReference = methodReference.parameters.single { it.name == "b" }
    val modified = methodReference.callBy(mapOf(parameterReference to true))
    return if (modified.b) {
        "OK"
    } else {
        "Fail"
    }
}
