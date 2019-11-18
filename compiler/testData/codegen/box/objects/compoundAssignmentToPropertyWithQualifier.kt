// IGNORE_BACKEND_FIR: JVM_IR
var log = ""

class A(p: String) {
    var pp = p

    init {
        log += "init($p);"
    }
}

operator fun A.plusAssign(s: String) {
    pp += s
    log += "pp = $pp;"
}

class D {
    val a = A("D")
}

object E {
    val t = A("E")
}

fun box(): String {
    log = ""
    val d = D()
    d.a += "foo"

    if (log != "init(D);pp = Dfoo;") return "1: log = \"$log\""

    log = ""
    E.t += "ET"

    if (log != "init(E);pp = EET;") return "2: log = \"$log\""

    log = ""
    val c = object { val b = object { val a = A("xcv") } }
    c.b.a += "eee"

    if (log != "init(xcv);pp = xcveee;") return "3: log = \"$log\""

    val b = object { val a = A("qwe") }
    b.a.pp += "ui"

    if (b.a.pp != "qweui") return "4: b.a.pp = \"${b.a.pp}\""

    return "OK"
}