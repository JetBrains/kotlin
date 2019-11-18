// IGNORE_BACKEND_FIR: JVM_IR
var log = ""

open class Base(val s: String)

class A(s: String) : Base(s) {
    constructor(i: Int) : this("O" + if (i == 23) {
            log += "logged1;"
            "K"
        }
        else {
            "fail"
        })

    constructor(i: Long) : this(if (i == 23L) {
        log += "logged2;"
        23
    }
    else {
        42
    })
}

class B : Base {
    constructor(i: Int) : super("O" + if (i == 23) {
        log += "logged3;"
        "K"
    }
    else {
        "fail"
    })
}

fun box(): String {
    var result = A(23).s
    if (result != "OK") return "fail1: $result"

    result = A(23L).s
    if (result != "OK") return "fail2: $result"

    result = B(23).s
    if (result != "OK") return "fail3: $result"

    if (log != "logged1;logged2;logged1;logged3;") return "fail log: $log"

    return "OK"
}