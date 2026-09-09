// FILE: JavaBase.java
package base;

public class JavaBase {
    protected void member() {
    }
}

// FILE: main.kt
package other

import base.JavaBase

class <caret>Child : JavaBase() {
    fun useSite() = member()
}

// callable: base/JavaBase.member
