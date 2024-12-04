// TARGET_BACKEND: JVM_IR
// Field VS property: case "lateinit"

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL";

    public String fieldValue() {
        return a;
    }
}

// FILE: Derived.kt
class Derived : BaseJava() {
    lateinit var a: String
}

fun box(): String {
    val d = Derived()
    d.a = "OK"
    if ((d as BaseJava).a == "OK") return "FAIL (accidental shadowed field access #1)"
    if (d.fieldValue() == "OK") return "FAIL (accidental shadowed field access #2)"
    return d.a
}
