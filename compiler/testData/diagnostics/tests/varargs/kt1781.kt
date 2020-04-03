// FIR_IDENTICAL
// FILE: kotlin.kt
fun foo() {
  JavaClass()
  JavaClass("")
}

// FILE: JavaClass.java

public class JavaClass {
    public JavaClass() {  }

    public JavaClass(String... ss) {  }
}
