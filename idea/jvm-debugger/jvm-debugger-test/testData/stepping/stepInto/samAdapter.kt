// FILE: samAdapter.kt
package samAdapter

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
    runReadAction { 1 }
}

fun runReadAction(action: () -> Int): Int {
    return forTests.MyJavaClass.runReadAction<Int>(action)
}

// STEP_INTO: 8

// FILE: forTests/MyJavaClass.java
package forTests;

import org.jetbrains.annotations.NotNull;

public class MyJavaClass {
    public static <T> T runReadAction(@NotNull Computable<T> computation) {
        return computation.compute();
    }
}

// FILE: forTests/Computable.java
package forTests;

public interface Computable <T> {
    T compute();
}