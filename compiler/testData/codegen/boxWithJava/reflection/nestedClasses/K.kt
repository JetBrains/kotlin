import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("Inner", "Nested", "PrivateNested"), J::class.nestedClasses.map { it.simpleName!! }.toSortedList())

    return "OK"
}
