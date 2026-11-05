// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: common
// FILE: common.kt
@JvmInline
value class V(val x: Long)

// MODULE: jvm()()(common)
// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(4, V::class.members.size)
    assertEquals(listOf("equals", "hashCode", "toString", "x"), V::class.members.map { it.name }.sorted())
    return "OK"
}
