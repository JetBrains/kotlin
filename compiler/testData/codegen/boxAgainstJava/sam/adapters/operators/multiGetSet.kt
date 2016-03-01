// FILE: JavaClass.java

class JavaClass {
    int get(Runnable i1, Runnable i2) {
        i1.run();
        i2.run();
        return 239;
    }

    void set(Runnable i1, Runnable i2, Runnable value) {
        i1.run();
        i2.run();
        value.run();
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v1 = "FAIL"
    obj[{ v1 = "O" }, { v1 += "K" }]
    if (v1 != "OK") return "get: $v1"

    var v2 = "FAIL"
    obj[{ v2 = "" }, { v2 += "O" }] = { v2 += "K" }
    if (v2 != "OK") return "set: $v2"

    return "OK"
}
