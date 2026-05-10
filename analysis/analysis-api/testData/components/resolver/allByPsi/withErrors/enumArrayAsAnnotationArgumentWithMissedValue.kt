// FILE: MyJavaAnnotation.java
public @interface MyJavaAnnotation {
    MyJavaEnum[] test();
    String value();
}

// FILE: MyJavaEnum.java
public enum MyJavaEnum {
    FOO, BAR
}

// FILE: main.kt
@MyJavaAnnotation(test = [])
fun main() {
}
