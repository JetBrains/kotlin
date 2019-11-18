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

inline fun <T, R> T.foo(f: (T) -> R) = f(this)

fun <T, R> T.bar(f: (T) -> R) = f(this)

fun box(): String {
    "rrr".foo { A(it) } += "aaa"

    if (log != "init(rrr);pp = rrraaa;") return "1: log = \"$log\""

    log = ""
    "foo".bar { A(it) } += "baaar"

    if (log != "init(foo);pp = foobaaar;") return "2: log = \"$log\""

    return "OK"
}