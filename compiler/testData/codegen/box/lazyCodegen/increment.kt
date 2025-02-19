// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

var holder = ""
var globalA: A = A(-1)
    get(): A {
        holder += "getA"
        return field
    }


class A(val p: Int) {

    var prop = this

    operator fun inc(): A {
        return A(p+1)
    }


}

fun box(): String {
    var a = A(1)
    ++a
    if (a.p != 2) return "fail 1: ${a.p} $holder"

    globalA = A(1)
    ++(globalA.prop)
    val holderValue = holder;
    if (globalA.p != 1 || globalA.prop.p != 2 || holderValue != "getA") return "fail 2: ${a.p} ${a.prop.p} ${holderValue}"

    return "OK"
}
