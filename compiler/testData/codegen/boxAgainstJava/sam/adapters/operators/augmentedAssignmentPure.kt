// FILE: JavaClass.java

class JavaClass {
    void plusAssign(Runnable i) {
        i.run();
    }

    void minusAssign(Runnable i) {
        i.run();
    }

    void timesAssign(Runnable i) {
        i.run();
    }

    void divAssign(Runnable i) {
        i.run();
    }

    void remAssign(Runnable i) {
        i.run();
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v1 = "FAIL"
    obj += { v1 = "OK" }
    if (v1 != "OK") return "plus: $v1"

    var v2 = "FAIL"
    obj -= { v2 = "OK" }
    if (v2 != "OK") return "minus: $v2"

    var v3 = "FAIL"
    obj *= { v3 = "OK" }
    if (v3 != "OK") return "times: $v3"

    var v4 = "FAIL"
    obj /= { v4 = "OK" }
    if (v4 != "OK") return "div: $v4"

    var v5 = "FAIL"
    obj %= { v5 = "OK" }
    if (v5 != "OK") return "mod: $v5"

    return "OK"
}
