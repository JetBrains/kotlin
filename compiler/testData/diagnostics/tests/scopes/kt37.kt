//KT-37 Typechecker doesn't complain about accessing non-public property
package kt37

class C() {
    private var f: Int

    {
        f = 610
    }
}

fun box(): String {
    val c = C()
    if (c.<!INVISIBLE_MEMBER!>f<!> != 610) return "fail"
    return "OK"
}