// DUMP_IR
// ISSUE: KT-66515

interface A
abstract class B

fun process(a: A): String {
    var b: B? = null
    if (a is B) {
        b = a
        return "OK"
    } else {
        return "FAIL"
    }
}

class C : B(), A

fun box(): String {
    return process(C())
}
