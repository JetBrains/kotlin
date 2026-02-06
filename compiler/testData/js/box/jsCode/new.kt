package foo

fun box(): String {
    js("function Constructor(a) { this.a = a; }")

    assertEquals(123, js("new Constructor(123)").a)
    assertEquals(123, js("new (Constructor)(123)").a)
    assertEquals(123, js("new (function(a) { this.a = a })(123)").a)
    assertEquals(js("(undefined)"), js("(new Constructor).a"))
    assertEquals(js("(undefined)"), js("(new (Constructor)).a"))

    return "OK"
}