// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS
//   ^ test checks reflection for synthetic classes
// MODULE: lib
// FILE: JavaClass.java

import java.util.Arrays;
import java.util.Comparator;

class JavaClass {
    public static String foo(Comparator<String> comparator) {
        return Arrays.toString(comparator.getClass().getGenericInterfaces());
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val supertypes = JavaClass.foo { a, b -> a.compareTo(b) }
    if (supertypes != "[interface java.util.Comparator]") return "Fail: $supertypes"
    return "OK"
}
