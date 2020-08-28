// FILE: lib/Application.java
package lib;

import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;

public interface Application {
    static Application getInstance() {
        return new Application() {
            @Override
            public void runReadAction(@NotNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public <T> T runReadAction(@NotNull Fun<T> computation) {
                return computation.invoke();
            }
        };
    }

    void runReadAction(@NotNull Runnable runnable);

    <T> T runReadAction(@NotNull Fun<T> computation);
}

// FILE: lib/Fun.java
package lib;

public interface Fun<T> {
    T invoke();
}

// FILE: lib/foo.kt
package lib

inline fun <R> runReadAction(crossinline runnable: () -> R): R {
    return Application.getInstance().runReadAction(Fun { runnable() })
}


// FILE: test.kt
package test

import lib.*

fun main() {
    //Breakpoint!
    println(runReadAction { "hello" })
}

// EXPRESSION: runReadAction { "hello" }
// RESULT: "hello": Ljava/lang/String;