import kotlin.test.*



fun box() {
    // WARNING
    // DO NOT REFORMAT AS TESTS MAY FAIL DUE TO INDENTATION CHANGE

    assertEquals("123", """
    123
    """.trimIndent())

    assertEquals("123\n   456", """
    123
       456
       """.trimIndent())

    assertEquals("   123\n456", """
       123
    456
    """.trimIndent())

    assertEquals("     123\n  456", """
       123
    456
    """.replaceIndent(newIndent = "  "))

    assertEquals("   123\n456", """
       123
    456""".trimIndent())

    assertEquals("    ", """
${"    "}
    """.trimIndent())

    val deindented = """
                                                        ,.
                  ,.                     _       oo.   `88P
                 ]88b              ,o.  d88.    ]88b     '
                  888   _          Y888o888     d88P     _     _
                  888 ,888          `Y88888o_  ,888    d88b   d88._____
                  888,888P ,oooooo.   ;888888b.]88P    888'   d888888888p
                  888888P d88888888.  J88b'YPP ]88b   ,888    d888P'''888.
                  8888P' ]88P   `888  d88[     d88P   ]88b    888'    Y88b
                  8888p  ]88b    888  888      d88[    888    888.    `888
                 ,88888b  888[   888  888.     d88[    888.   Y88b     Y88[
                 d88PY88b `888L,d88P  Y88b     Y88b    ]88b   `888     888'
                 888  Y88b  Y88888P    888.     888.    888.   Y88b   `88P
                d88P   888   `'P'      Y888.    `888.   `88P   `Y8P     '
                Y8P'    '               `YP      Y8P'     '

                ____       dXp   _    _        _________
              ddXXXXXp     XXP  ,XX  dXb      Yo.XXXXXX      ,oooooo.
              X'L_oXXP     XX'   XX[ dXb      dXb            YPPPPXXX'
              XYXXXXX     ]XX    dXb dXb      dX8Xooooo         dXXP
              XXb`YYXXo.   YXXo_ dXP dXP      YXb''''''       ,XXP'
              `XX   `YYXb   `YXXXXP  XX[      ]XX            ,XX'
               YXb     YXb     `''   XXXXooL  `XX._____      `XXXXXXXXooooo.
               `XP      '             ''''''   YPXXXXXX'       ''''''`''YPPP
    """.trimIndent()

    assertEquals(23, deindented.lines().size)
    val indents = deindented.lines().map {
        var i = 0
        while (i < it.length && it[i].isWhitespace())
            i++
        return@map i
    }
    assertEquals(0, indents.min())
    assertEquals(42, indents.max())
    assertEquals(1, deindented.lines().count { it.isEmpty() })
}
