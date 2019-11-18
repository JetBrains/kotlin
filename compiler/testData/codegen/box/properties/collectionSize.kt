// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Test.java

public class Test extends java.util.ArrayList<String> {
    public final int size() {
        return 56;
    }
}

// FILE: test.kt

class OurTest : Test()

fun box(): String {
    val t = OurTest()
    val x: MutableCollection<String> = t

    if (t.size != 56) return "fail 1: ${t.size}"
    if (x.size != 56) return "fail 1: ${x.size}"

    return "OK"
}
