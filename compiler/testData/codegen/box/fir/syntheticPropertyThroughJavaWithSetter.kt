// TARGET_BACKEND: JVM_IR
// ISSUE: KT-59550

// FILE: Intermediate.java
public class Intermediate extends Base {
    public Intermediate(String foo) {
        super(foo);
    }
}

// FILE: FinalAndBase.kt
abstract class Base(private var foo: String) {
    fun getFoo() = foo

    fun setFoo(newFoo: String) {
        foo = newFoo
    }
}

class Final(val i: Intermediate) : Intermediate(i.foo)

fun box(): String {
    val f = Final(Intermediate(""))
    f.foo = "OK"
    return f.foo
}
