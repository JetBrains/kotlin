// CLASS_ID: kotlin/collections/Iterable
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false

// FILE: JavaClass.java
import java.util.List

class JavaClass {
    public static List<String> getNames() {
        throw Exception()
    }
}

// FILE: test.kt
val v<caret>alue = JavaClass.getNames()
