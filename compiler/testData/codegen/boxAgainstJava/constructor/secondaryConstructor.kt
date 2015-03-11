import test.secondaryConstructor;

class Child : secondaryConstructor {
    constructor(): super() {}
    constructor(x: String): super(x) {}
    constructor(x: String, y: String): super(x, y) {}
}

fun box(): String {
    val c1 = Child().toString()
    if (c1 != "def_x#def_y") return "fail1: $c1"

    val c2 = Child("abc").toString()
    if (c2 != "abc#def_y") return "fail2: $c2"

    val c3 = Child("abc", "def").toString()
    if (c3 != "abc#def") return "fail3: $c3"
    return "OK"
}
