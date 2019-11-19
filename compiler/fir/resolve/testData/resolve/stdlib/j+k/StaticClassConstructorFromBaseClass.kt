// FILE: AbstractClass.java

public abstract class AbstractClass {
    public static class StaticClass {

    }
}

// FILE: User.kt

class User : AbstractClass() {
    fun foo() {
        val sc = <!UNRESOLVED_REFERENCE!>StaticClass<!>()
    }
}

fun test() {
    AbstractClass.<!UNRESOLVED_REFERENCE!>StaticClass<!>()
}