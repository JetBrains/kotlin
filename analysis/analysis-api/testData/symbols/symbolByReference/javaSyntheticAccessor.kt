// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: main.kt
fun some() {
  JavaClass().f<caret>oo;
}

// FILE: JavaClass.java
public class JavaClass {
  public int getFoo() { return 1; };
}
