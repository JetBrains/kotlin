// TARGET_BACKEND: JVM_IR
// ISSUE: KT-59550 (related)

// MODULE: m1
// FILE: Base.kt
abstract class Base(internal val foo: String) {
    fun getFoo() = foo
}

// MODULE: m2(m1)
// FILE: Intermediate.java
public class Intermediate extends Base {
    public Intermediate(String foo) {
        super(foo);
    }
}

// FILE: FinalAndBase.kt

class Final(val i: Intermediate) : Intermediate(i.foo)

fun box(): String = Final(Intermediate("OK")).foo
