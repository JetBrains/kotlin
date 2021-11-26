// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// FILE: F.java
import java.util.function.Function;

public class F {
    public static <T, U> U passNull(Function<? super T, ? extends U> f) {
        return f.apply(null);
    }
}

// FILE: test.kt
fun test(f: (Int?) -> String): String {
    return F.passNull(f)
}

fun box(): String {
    return test {
        it?.toString() ?: "OK"
    }
}
