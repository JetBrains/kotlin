// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

var holder = ""

fun getA(): A {
    holder += "getA()"
    return A
}

object A {

    @JvmStatic var a: Int = 1

    var b: Int = 1
        @JvmStatic get

    var c: Int = 1
        @JvmStatic set

}


fun box(): String {

    if (getA().a != 1 || holder != "getA()") return "fail 1: $holder"
    holder = ""

    if (getA().b != 1 || holder != "getA()") return "fail 2: $holder"
    holder = ""

    if (getA().c != 1 || holder != "getA()") return "fail 3: $holder"
    holder = ""

    getA().a = 2
    if (getA().a != 2 || holder != "getA()getA()") return "fail 1: $holder"
    holder = ""

    getA().b = 2
    if (getA().b != 2 || holder != "getA()getA()") return "fail 2: $holder"
    holder = ""

    getA().c = 2
    if (getA().c != 2 || holder != "getA()getA()") return "fail 3: $holder"
    holder = ""

    return "OK"
}
