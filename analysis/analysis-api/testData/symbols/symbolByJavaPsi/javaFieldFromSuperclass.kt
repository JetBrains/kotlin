// FILE: main.kt
fun some() {
    JavaClass().<caret>count
}

// FILE: SuperClass.java
public class SuperClass {
    public Integer count = 0;
}

// FILE: JavaClass.java
public class JavaClass extends SuperClass {
}
