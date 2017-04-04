import kotlin.test.*



fun box() {
    val input = "AbbabA"
    assertEquals("Abb${'$'}bA", input.replaceFirst('a','$'))
    assertEquals("${'$'}bbabA", input.replaceFirst('a','$', ignoreCase = true))
    // doesn't pass in Rhino JS
    // assertEquals("schrodinger", "schrÖdinger".replaceFirst('ö', 'o', ignoreCase = true))

    assertEquals("Abba${'$'}", input.replaceFirst("bA", "$"))
    assertEquals("Ab${'$'}bA", input.replaceFirst("bA", "$", ignoreCase = true))

    assertEquals("-test", "test".replaceFirst("", "-"))
}
