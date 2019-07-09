// FILE: JavaClass.java

import org.jetbrains.annotations.NotNull;

class JavaClass {
    @NotNull JavaClass plus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull JavaClass minus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull JavaClass times(Runnable i) {
        i.run();
        return this;
    }

    @NotNull JavaClass div(Runnable i) {
        i.run();
        return this;
    }

    @NotNull JavaClass rem(Runnable i) {
        i.run();
        return this;
    }
}

// FILE: 1.kt

fun box(): String {
    var obj = JavaClass()

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
