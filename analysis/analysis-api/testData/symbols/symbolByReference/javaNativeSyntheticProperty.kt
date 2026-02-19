// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: main.kt
fun some() {
    JavaClass().<caret>count
}

// FILE: JavaClass.java
public class JavaClass {
    public native int getCount();
    public native void setCount(int x);
}
