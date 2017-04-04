import kotlin.test.*



fun box() {
    // WARNING
    // DO NOT REFORMAT AS TESTS MAY FAIL DUE TO INDENTATION CHANGE

    assertEquals("ABC\n123\n456", """ABC
                                  |123
                                  |456""".trimMargin())

    assertEquals("ABC\n  123\n  456", """ABC
                                  |123
                                  |456""".replaceIndentByMargin(newIndent = "  "))

    assertEquals("ABC \n123\n456", """ABC${" "}
                                  |123
                                  |456""".trimMargin())

    assertEquals(" ABC\n123\n456", """ ABC
                                    >>123
                                    ${"\t"}>>456""".trimMargin(">>"))

    assertEquals("", "".trimMargin())

    assertEquals("", """
                        """.trimMargin())

    assertEquals("", """
                        |""".trimMargin())

    assertEquals("", """
                        |
                        """.trimMargin())

    assertEquals("    a", """
        |    a
    """.trimMargin())

    assertEquals("    a", """
        |    a""".trimMargin())

    assertEquals("    a", """ |    a
    """.trimMargin())

    assertEquals("    a", """ |    a""".trimMargin())

    assertEquals("\u0000|ABC", "${"\u0000"}|ABC".trimMargin())
}
