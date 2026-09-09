// FILE: JavaBase.java
package base;

public class JavaBase {
    void member() {
    }
}

// FILE: main.kt
package base

class <caret>Child : JavaBase() {
    fun useSite() = member()
}

// callable: base/JavaBase.member
