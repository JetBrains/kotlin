// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-66020

// FILE: Base.kt
open class Base {
    open val b = "O"

    @JvmName("getBJava")
    fun getB() : String = "K"
}

// FILE: Derived.java
public class Derived extends Base {
    public static String box() {
        Impl x = new Impl();
        return x.getB() + x.getBJava();
    }
}

// FILE: Impl.kt
class Impl : Derived()

fun box(): String {
    return Derived.box()
}
