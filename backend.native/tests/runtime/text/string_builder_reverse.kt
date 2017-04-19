fun assertEquals(builder: StringBuilder, content: String) {
    if (builder.toString() != content) throw AssertionError("Test failed: $builder != $content")
}

fun assertTrue(condition: Boolean) {
    if (!condition) throw AssertionError("The condition is false")
}

fun reverseTest(original: String, reversed: String, reversedBack: String) {
    assertEquals(StringBuilder(original).reverse(), reversed)
    assertEquals(StringBuilder(reversed).reverse(), reversedBack)
}


fun main(args: Array<String>) {
    var builder = StringBuilder("123456")
    assertTrue(builder === builder.reverse())
    assertEquals(builder, "654321")

    builder.length = 1
    assertEquals(builder, "6")

    builder.length = 0
    assertEquals(builder, "")

    var str: String = "a"
    reverseTest(str, str, str)

    str = "ab"
    reverseTest(str, "ba", str)

    str = "abcdef"
    reverseTest(str, "fedcba", str)

    str = "abcdefg"
    reverseTest(str, "gfedcba", str)

    str = "\ud800\udc00"
    reverseTest(str, str, str)

    str = "\udc00\ud800"
    reverseTest(str, "\ud800\udc00", "\ud800\udc00")

    str = "a\ud800\udc00"
    reverseTest(str, "\ud800\udc00a", str)

    str = "ab\ud800\udc00"
    reverseTest(str, "\ud800\udc00ba", str)

    str = "abc\ud800\udc00"
    reverseTest(str, "\ud800\udc00cba", str)

    str = "\ud800\udc00\udc01\ud801\ud802\udc02"
    reverseTest(str, "\ud802\udc02\ud801\udc01\ud800\udc00",
            "\ud800\udc00\ud801\udc01\ud802\udc02")

    str = "\ud800\udc00\ud801\udc01\ud802\udc02"
    reverseTest(str, "\ud802\udc02\ud801\udc01\ud800\udc00", str)

    str = "\ud800\udc00\udc01\ud801a"
    reverseTest(str, "a\ud801\udc01\ud800\udc00",
            "\ud800\udc00\ud801\udc01a")

    str = "a\ud800\udc00\ud801\udc01"
    reverseTest(str, "\ud801\udc01\ud800\udc00a", str)

    str = "\ud800\udc00\udc01\ud801ab"
    reverseTest(str, "ba\ud801\udc01\ud800\udc00",
            "\ud800\udc00\ud801\udc01ab")

    str = "ab\ud800\udc00\ud801\udc01"
    reverseTest(str, "\ud801\udc01\ud800\udc00ba", str)

    str = "\ud800\udc00\ud801\udc01"
    reverseTest(str, "\ud801\udc01\ud800\udc00", str)

    str = "a\ud800\udc00z\ud801\udc01"
    reverseTest(str, "\ud801\udc01z\ud800\udc00a", str)

    str = "a\ud800\udc00bz\ud801\udc01"
    reverseTest(str, "\ud801\udc01zb\ud800\udc00a", str)

    str = "abc\ud802\udc02\ud801\udc01\ud800\udc00"
    reverseTest(str, "\ud800\udc00\ud801\udc01\ud802\udc02cba", str)

    str = "abcd\ud802\udc02\ud801\udc01\ud800\udc00"
    reverseTest(str, "\ud800\udc00\ud801\udc01\ud802\udc02dcba", str)

    println("OK")
}