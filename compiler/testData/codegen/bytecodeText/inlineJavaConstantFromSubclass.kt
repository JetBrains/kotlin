// IGNORE_BACKEND: JVM_IR
// FILE: first/JavaSuperclass.java

package first;

public class JavaSuperclass {
    public static final String CONSTANT = "foo";
}

// FILE: first/JavaSubclass.java

package first;

public class JavaSubclass extends JavaSuperclass {
}

// FILE: second/bar.kt

package second

import first.JavaSubclass

fun bar() = JavaSubclass.CONSTANT

// @second/BarKt.class
// 0 INVOKESTATIC
// 0 GETSTATIC
// 1 LDC "foo"