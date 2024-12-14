// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-59550
// LATEST_LV_DIFFERENCE
// FILE: Base.kt
abstract class Base(private val foo: String) {
    fun getFoo(): String = foo
}

// FILE: Intermediate.java
public class Intermediate extends Base {
    public Intermediate(String foo) {
        super(foo);
    }
}

// FILE: main.kt
class Final(val i: Intermediate) : Intermediate(i.foo)

fun test(x: Final) {
    x.foo
}
