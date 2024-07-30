// MODULE: main
// FILE: JavaClass.java
import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface MyAnnotation {
  Class<?> typeParam();
}
class JavaClass extends @MyAnnotation(typeParam = String.class) Object {}

// FILE: main.kt
fun test() {
  val x: J<caret>avaClass = TODO()
}
