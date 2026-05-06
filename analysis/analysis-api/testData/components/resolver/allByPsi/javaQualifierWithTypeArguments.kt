// FILE: JavaClass.java
public class JavaClass<T> {
    public static Integer field = 1;
}

// FILE: main.kt
fun test() {
   val p = JavaClass<Int>.field
}
