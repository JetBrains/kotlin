import kotlin.reflect.*
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.members.map { it.name }.sorted())
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.functions.map { it.name }.sorted())
    assertEquals(listOf("member", "staticMethod"), J::class.declaredFunctions.map { it.name }.sorted())

    assertEquals(1, J::class.constructors.size)

    return "OK"
}
