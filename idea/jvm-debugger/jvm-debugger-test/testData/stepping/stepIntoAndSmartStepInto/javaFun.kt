// FILE: javaFun.kt
package javaFun

import forTests.MyJavaClass

fun main(args: Array<String>) {
    val klass = MyJavaClass()
    //Breakpoint!
    klass.testFun()
}

// FILE: forTests/MyJavaClass.java
package forTests;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public class MyJavaClass {
    public void testFun() {
        int i = 1;
    }

    public MyJavaClass() {}
}