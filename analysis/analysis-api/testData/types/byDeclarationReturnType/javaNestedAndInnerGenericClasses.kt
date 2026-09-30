// FILE: JavaOuter.java
public class JavaOuter<AA> {
    public static class Nested<BB> {
        public class Inner<CC> {}
    }
}

// FILE: main.kt
fun fo<caret>o(): JavaOuter.Nested<String>.Inner<Int>? = null
