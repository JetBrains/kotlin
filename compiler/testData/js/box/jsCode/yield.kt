package foo

fun box(): String {
    val generator = js("""(
        function*() {
            yield "1";
            yield;
            var value = yield 2;
            var undef = yield;
            yield* [3, 4];
        }
    )""")

    val sequence = generator()
    assertEquals("1", sequence.next().value)
    assertEquals(js("undefined"), sequence.next().value)
    assertEquals(2, sequence.next().value)
    assertEquals(js("undefined"), sequence.next().value)
    assertEquals(3, sequence.next().value)
    assertEquals(4, sequence.next().value)

    return "OK"
}