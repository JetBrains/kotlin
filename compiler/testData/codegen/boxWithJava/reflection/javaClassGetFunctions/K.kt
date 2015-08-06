import kotlin.reflect.*
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.members.map { it.name }.toSortedList())
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.functions.map { it.name }.toSortedList())
    assertEquals(listOf("member", "staticMethod"), J::class.declaredFunctions.map { it.name }.toSortedList())

    assertEquals(1, J::class.constructors.size())

    return "OK"
}
