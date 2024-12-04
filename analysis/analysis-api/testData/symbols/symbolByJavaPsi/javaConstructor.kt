// FILE: main.kt
fun some() {
    JavaCla<caret>ss(5)
}

// FILE: JavaClass.java
public class JavaClass {
    public Integer count = 0;

    JavaClass(int _count) {
        count = _count;
    }
}
