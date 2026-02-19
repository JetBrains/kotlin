// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: main.kt
abstract class SomeKotlinClass {
    abstract val String.foo: Int
}

fun JavaClass.usage() {
    "str".f<caret>oo
}

// FILE: JavaClass.java
public class JavaClass extends SomeKotlinClass {
    @Override
    public int getFoo(String $this$foo) {
        return 0;
    }
}
