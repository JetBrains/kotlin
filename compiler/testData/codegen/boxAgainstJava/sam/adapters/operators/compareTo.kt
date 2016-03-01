// FILE: JavaClass.java

class JavaClass {
    int compareTo(Runnable i) {
        i.run();
        return 239;
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v1 = "FAIL"
    obj < { v1 = "OK" }
    if (v1 != "OK") return "<: $v1"

    var v2 = "FAIL"
    obj > { v2 = "OK" }
    if (v2 != "OK") return ">: $v2"

    var v3 = "FAIL"
    obj <= { v3 = "OK" }
    if (v3 != "OK") return "<=: $v3"

    var v4 = "FAIL"
    obj >= { v4 = "OK" }
    if (v4 != "OK") return ">=: $v4"

    return "OK"
}
