// FILE: JavaClass.java

import java.util.Arrays;
import java.util.Comparator;

class JavaClass {
    public static String foo(Comparator<String> comparator) {
        return Arrays.toString(comparator.getClass().getGenericInterfaces());
    }
}

// FILE: 1.kt

fun box(): String {
    val supertypes = JavaClass.foo { a, b -> a.compareTo(b) }
    if (supertypes != "[java.util.Comparator<java.lang.String>]") return "Fail: $supertypes"
    return "OK"
}
