// FILE: checkNotNull.kt
package checkNotNull

import forTests.MyJavaClass

fun main(args: Array<String>) {
    val myClass = MyJavaClass()
    //Breakpoint!
    val a: String = myClass.testNotNullFun()
    val b = 1
}

// STEP_INTO: 3

// FILE: forTests/MyJavaClass.java
package forTests;

import org.jetbrains.annotations.NotNull;

public class MyJavaClass {
    @NotNull
    public String testNotNullFun() {
        return "a";
    }

    public MyJavaClass() {}
}
