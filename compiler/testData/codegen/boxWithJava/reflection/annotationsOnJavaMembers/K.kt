import kotlin.test.assertEquals

annotation class Anno(val value: String)

fun box(): String {
    assertEquals("[@Anno(value=J)]", J::class.annotations.toString())
    assertEquals("[@Anno(value=foo)]", J::foo.annotations.toString())
    assertEquals("[@Anno(value=bar)]", J::bar.annotations.toString())
    assertEquals("[@Anno(value=constructor)]", ::J.annotations.toString())

    return "OK"
}
