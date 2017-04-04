import kotlin.test.*



fun box() {
    val input = "abbAb"
    assertEquals("abb${'$'}b", input.replace('A', '$'))
    assertEquals("/bb/b", input.replace('A', '/', ignoreCase = true))

    assertEquals("${'$'}bAb", input.replace("ab", "$"))
    assertEquals("/b/", input.replace("ab", "/", ignoreCase = true))

    assertEquals("-a-b-b-A-b-", input.replace("", "-"))
}
