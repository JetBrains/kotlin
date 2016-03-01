// FILE: Container.java

import org.jetbrains.annotations.NotNull;

class Container {
    @NotNull
    Value get(Runnable i) {
        i.run();
        return new Value();
    }

    void set(Runnable i, @NotNull Value value) {
        i.run();
    }
}

class Value {
    @NotNull Value plus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value minus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value times(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value div(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value mod(Runnable i) {
        i.run();
        return this;
    }
}

// FILE: 1.kt

fun box(): String {
    var c = Container()
    var indexAccess = 0

    var v1 = "FAIL"
    c[{ indexAccess++ }] += { v1 = "OK" }
    if (v1 != "OK") return "plus: $v1"

    var v2 = "FAIL"
    c[{ indexAccess++ }] -= { v2 = "OK" }
    if (v2 != "OK") return "minus: $v2"

    var v3 = "FAIL"
    c[{ indexAccess++ }] *= { v3 = "OK" }
    if (v3 != "OK") return "times: $v3"

    var v4 = "FAIL"
    c[{ indexAccess++ }] /= { v4 = "OK" }
    if (v4 != "OK") return "div: $v4"

    var v5 = "FAIL"
    c[{ indexAccess++ }] %= { v5 = "OK" }
    if (v5 != "OK") return "mod: $v5"

    if (indexAccess != 10) return "Fail: $indexAccess"

    return "OK"
}
