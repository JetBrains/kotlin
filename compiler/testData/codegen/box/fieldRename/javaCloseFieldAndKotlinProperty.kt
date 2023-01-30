// TARGET_BACKEND: JVM_IR
// Field VS property: case 3.1
// See KT-54393 for details

// FILE: VeryBase.kt
open class VeryBase {
    val some = "FAIL"
}

// FILE: Base.java
public class Base extends VeryBase {
    public String some = "OK";

    public String foo() {
        return some;
    }
}

// FILE: Test.kt
class Derived : Base()

fun box(): String {
    val first = Derived().some
    if (first != "OK") return first
    val d = Derived()
    if (d::some.get() != "OK") return d::some.get()
    d.some = "12"
    if (d.foo() != "12") return "Error writing: ${d.foo()}"
    return "OK"
}
