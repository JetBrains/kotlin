// IGNORE_FIR
// FILE: main.kt
package pkg

fun test(c: UpperBound) {
    <expr>Util.test(c)</expr>
}

abstract class Foo<A : UpperBound>

abstract class UpperBound

// FILE: Util.java
import org.jetbrains.annotations.NotNull;

public class Util {
    @NotNull
    public static <A2 extends pkg.UpperBound> pkg.Foo<A2> test(@NotNull A2 args) {
        return "";
    }
}
