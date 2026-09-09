// FILE: JavaBase.java
package base;

public class JavaBase {
    private void member() {
    }
}

// FILE: main.kt
package base

class <caret>Child : JavaBase()

// callable: base/JavaBase.member
