// FILE: JavaClass.java
public class JavaClass<T> {
    public static Integer field = 1;
    public static String method() {}
}

// FILE: main.kt
fun test() {
   val p = JavaClass<Int>.field
   val m = JavaClass<Long>.method()
}
