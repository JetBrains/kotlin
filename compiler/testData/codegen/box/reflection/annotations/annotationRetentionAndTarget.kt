// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

fun box(): String {
    val a = Anno::class.annotations
    assertEquals(listOf(
        "@kotlin.annotation.Retention(RUNTIME)",
        "@kotlin.annotation.Target(allowedTargets={CLASS})",
    ), a.map {
        // JDK 11+ renders arrays as "{...}" instead of "[...]"
        // JDK 17+ doesn't render "value="
        it.toString().replace('[', '{').replace(']', '}').replace("value=", "")
    }.sorted())

    return "OK"
}
