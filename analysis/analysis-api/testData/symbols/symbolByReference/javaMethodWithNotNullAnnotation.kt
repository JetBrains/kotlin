// FILE: main.kt
fun some() {
  JavaClass().f<caret>oo();
}
// FILE: JavaClass.java
import import org.jetbrains.annotations.NotNull;

public class JavaClass {
  @NotNull
  public String foo() {};
}
