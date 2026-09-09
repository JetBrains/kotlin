// FILE: JavaBase.java
package base;

public class JavaBase {
    int member = 42;
}

// FILE: main.kt
package base

class <caret>Child : JavaBase() {
    fun useSite(): Int = member
}

// callable: base/JavaBase.member
