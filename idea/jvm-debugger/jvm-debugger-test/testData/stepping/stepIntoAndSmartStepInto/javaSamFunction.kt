// FILE: javaSamFunction.kt
package javaSamFunction

import forTests.MyJavaClass

fun main(args: Array<String>) {
    val klass = MyJavaClass()
    //Breakpoint!
    klass.other { /* do nothing*/ }
}

// FILE: forTests/MyJavaClass.java
package forTests;

public class MyJavaClass {
    public void other(Runnable runnable) {
        runnable.run();
    }

    public MyJavaClass() {}
}