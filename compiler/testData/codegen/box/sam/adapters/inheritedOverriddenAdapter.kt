// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: Super.java

class Super {
    public String lastCalled = null;

    void foo(Runnable r) {
        lastCalled = "super";
    }
}

// FILE: Sub.java

import kotlin.jvm.functions.Function0;
import kotlin.Unit;

class Sub extends Super {
    void foo(Function0<Unit> r) {
        lastCalled = "sub";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val sub = Sub()
    val sup: Super = sub

    sup.foo{ }
    if (sub.lastCalled != "super") {
        return "FAIL: ${sub.lastCalled} instead of super"
    }

    sub.foo{ }
    if (sub.lastCalled != "sub") {
        return "FAIL: ${sub.lastCalled} instead of sub"
    }

    return "OK"
}
