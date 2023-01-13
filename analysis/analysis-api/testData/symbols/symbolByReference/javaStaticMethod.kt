// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: main.kt
fun some() {
  JavaClass.f<caret>oo();
}

// FILE: JavaClass.java
public class JavaClass {
  public static void foo() {};
}
