import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.String!", J::string.returnType.toString())
    assertEquals("kotlin.collections.(Mutable)List<kotlin.Any!>!", J::list.returnType.toString())

    return "OK"
}
