// ISSUE: KT-50850
// FILE: JavaBase.java
public interface JavaBase {
    void delete(String s);
}

// FILE: main.kt
abstract class KotlinBase {
    open fun delete(s: String) {}
}

class InterfaceThenClass : JavaBase, KotlinBase() {}
class ClassThenInterface : KotlinBase(), JavaBase {}

fun test_1(x: InterfaceThenClass, s: String?) {
    x.delete(s)
}

fun test_2(x: ClassThenInterface, s: String?) {
    x.delete(<!TYPE_MISMATCH!>s<!>)
}
