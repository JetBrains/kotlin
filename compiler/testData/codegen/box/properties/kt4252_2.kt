class Foo() {
    default object {
        val bar = "OK";
        var boo = "FAIL";
    }

    val a = bar
    var b = Foo.bar
    val c: String
    var d: String

    {
        c = bar
        d = Foo.bar
        boo = "O"
        Foo.boo += "K"
    }
}

fun box(): String {
    val foo = Foo()

    if (foo.a != "OK") return "foo.a != OK"
    if (foo.b != "OK") return "foo.b != OK"
    if (foo.c != "OK") return "foo.c != OK"
    if (foo.d != "OK") return "foo.d != OK"
    if (Foo.boo != "OK") return "Foo.boo != OK"

    return "OK"
}