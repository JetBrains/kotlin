// DO_NOT_CHECK_SYMBOL_RESTORE_K1

// FILE: main.kt
class J: JavaClass<String> {}

fun some(j: J) {
  j.f<caret>oo()
}

// FILE: JavaClass.java
public class JavaClass<T> {
  public <K extends T & Runnable> void foo() {};
}
