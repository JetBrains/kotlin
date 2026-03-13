// DO_NOT_CHECK_SYMBOL_RESTORE_K2
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

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