class A
class B

var holder = 0

fun A.not(): A {
    holder++
    return this;
}

fun B.not(): Boolean {
    holder++
    return false;
}

fun box(): String {
    !!!!!A()
    if (holder != 5) return "fail 1"

    holder = 0;
    if (!!!B() || holder != 1) return "fail 2"

    if (!B() != false) return "fail 3"

    if (!!B() != true) return "fail 4"

    return "OK"
}