// WITH_RUNTIME
// FILE: J.java

import kotlin.Function;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class J {
    public static String test(Function<String> x) {
        if (x instanceof Function1) return "Fail 1";
        if (x instanceof Function2) return "Fail 2";
        if (!(x instanceof Function0)) return "Fail 3";

        return ((Function0<String>) x).invoke();
    }
}

// FILE: K.kt

fun box(): String {
    return J.test({ "OK" })
}
