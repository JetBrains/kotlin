import test.*

class A {
    var result = 0;

    var field: Int
        get() {
            doCall { return 1 }
            return 2
        }
        set(v: Int) {
            doCall {
                result = v / 2
                return
            }
            result = v
        }
}


fun box(): String {

    val a = A()
    if (a.field != 1) return "fail 1: ${a.field}"

    a.field = 4
    if (a.result != 2) return "fail 2: ${a.result}"

    return "OK"
}