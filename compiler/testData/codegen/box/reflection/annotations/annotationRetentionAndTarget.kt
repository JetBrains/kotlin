// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

fun box(): String {
    val a = Anno::class.annotations
    assertEquals(listOf(
        "@kotlin.annotation.Target(allowedTargets=[CLASS])",
        "@kotlin.annotation.Retention(value=RUNTIME)",
        "@java.lang.annotation.Retention(value=RUNTIME)",
        "@java.lang.annotation.Target(value=[TYPE])",
    ), a.map { it.toString() })

    return "OK"
}
