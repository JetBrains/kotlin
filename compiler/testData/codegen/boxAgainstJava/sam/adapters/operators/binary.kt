// IGNORE_BACKEND: JVM_IR
// FILE: JavaClass.java

class JavaClass {
    JavaClass plus(Runnable i) {
        i.run();
        return this;
    }

    JavaClass minus(Runnable i) {
        i.run();
        return this;
    }

    JavaClass times(Runnable i) {
        i.run();
        return this;
    }

    JavaClass div(Runnable i) {
        i.run();
        return this;
    }

    JavaClass rem(Runnable i) {
        i.run();
        return this;
    }

    JavaClass rangeTo(Runnable i) {
        i.run();
        return this;
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v1 = "FAIL"
    obj + { v1 = "OK" }
    if (v1 != "OK") return "plus: $v1"

    var v2 = "FAIL"
    obj - { v2 = "OK" }
    if (v2 != "OK") return "minus: $v2"

    var v3 = "FAIL"
    obj * { v3 = "OK" }
    if (v3 != "OK") return "times: $v3"

    var v4 = "FAIL"
    obj / { v4 = "OK" }
    if (v4 != "OK") return "div: $v4"

    var v5 = "FAIL"
    obj % { v5 = "OK" }
    if (v5 != "OK") return "mod: $v5"

    var v6 = "FAIL"
    obj .. { v6 = "OK" }
    if (v6 != "OK") return "rangeTo: $v6"

    return "OK"
}
