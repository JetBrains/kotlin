// FILE: JavaBase.java
package base;

public class JavaBase {
    public void member() {
    }
}

// FILE: main.kt
package other

import base.JavaBase

class <caret>Child : JavaBase() {
    fun useSite() = member()
}

// callable: base/JavaBase.member
