// FILE: main.kt
package test

class KSub : JavaBase<String>()

// callable: test/KSub.value

// FILE: JavaBase.java
package test;

public class JavaBase<T> {
    public T value;
}
