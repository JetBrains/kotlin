// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// FILE: JavaClass.java
import java.util.List

class JavaClass {
    public static List<String> getNames() {
        throw Exception()
    }
}

// FILE: test.kt
val v<caret_type1>1 = JavaClass.getNames()

val v<caret_type2>2: Iterable<String> = emptyList()
