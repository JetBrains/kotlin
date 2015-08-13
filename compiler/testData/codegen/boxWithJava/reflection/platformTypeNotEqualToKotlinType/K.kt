import kotlin.test.assertNotEquals

fun nonNullString(): String = ""
fun nullableString(): String? = ""

fun box(): String {
    assertNotEquals(J::foo.returnType, ::nonNullString.returnType)
    assertNotEquals(J::foo.returnType, ::nullableString.returnType)

    return "OK"
}
