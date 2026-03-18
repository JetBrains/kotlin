// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K2
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

// FILE: Base.kt
abstract class Base {
    fun getFoo(): String {
        return "foo"
    }
}

// FILE: Intermediate.java
public class Intermediate extends Base {}

// FILE: main.kt
fun test(obj: Intermediate) {
    obj.f<caret>oo
}