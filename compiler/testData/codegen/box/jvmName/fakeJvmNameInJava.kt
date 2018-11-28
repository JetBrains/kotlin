// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: FakePlatformName.java

import kotlin.jvm.JvmName;

public class FakePlatformName {
    @JvmName(name = "fake")
    public String foo() {
        return "foo";
    }

    public String fake() {
        return "fake";
    }
}

// FILE: FakePlatformName.kt

fun box(): String {
    val test1 = FakePlatformName().foo()
    if (test1 != "foo") return "Failed: FakePlatformName().foo()==$test1"

    return "OK"
}
