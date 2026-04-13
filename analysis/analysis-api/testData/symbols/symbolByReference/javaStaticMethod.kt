// FILE: main.kt
fun some() {
  JavaClass.f<caret>oo();
}

// FILE: JavaClass.java
public class JavaClass {
  public static void foo() {};
}
