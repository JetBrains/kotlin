// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: Derived.kt
fun usage() {
    Derived().some<caret>thing
}

class Derived : Base() {
    override fun getSomething(): String = "42"
    override fun setSomething(s: String) {

    }
}

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }

    public void setSomething(String s) {

    }
}
