// FILE: stopInInlineUnderSamConversion.kt
package stopInInlineUnderSamConversion

import forTests.SamConversion

fun main(args: Array<String>) {
    val a = 1

    SamConversion.doAction({
                               inlineCall {
                                   {
                                       //Breakpoint!
                                       foo(a)
                                   }()
                               }
                           })
}

fun foo(a: Any) {}

inline fun inlineCall(f: () -> Unit) {
    f()
}

// FILE: forTests/SamConversion.java
package forTests;

public class SamConversion {
    public interface Runnable {
        public abstract void run();
    }

    public static void doAction(Runnable runnable) {
        runnable.run();
    }
}