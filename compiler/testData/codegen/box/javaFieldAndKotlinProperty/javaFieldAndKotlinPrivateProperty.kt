// TARGET_BACKEND: JVM_IR
// Note: works accidentally via backing field access
// Field VS property: case 4.2
// More or less duplicates the case in KT-34943/KT-54393

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "OK"

    fun x() = a
}

fun box() = Derived().x()
