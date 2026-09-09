// FILE: JavaBase.java
package base;

public class JavaBase {
    void member() {
    }
}

// FILE: main.kt
package other

import base.JavaBase

class <caret>Child : JavaBase()

// callable: base/JavaBase.member
