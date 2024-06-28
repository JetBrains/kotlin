// ISSUE: KT-50850
// FILE: Base.java
public interface Base {
    void delete(String s);
}

// FILE: main.kt
abstract class Derived : Base {
    override fun delete(s: String) {}
}

class InterfaceThenClass : Base, Derived() {}

fun test_1(x: InterfaceThenClass, s: String?) {
    x.delete(s)
}

class ClassThenInterface : Derived(), Base {}

fun test_2(x: ClassThenInterface, s: String?) {
    x.delete(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
}
