// ARE_EQUAL: true
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: kotlin/collections/MutableList
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true

// FILE: JavaClass.java
package test;

import java.util.List;

class JavaClass {
    public static List<String> getNames() {
        throw Exception();
    }
}

// FILE: test.kt
package test

val v<caret_type1>1 = JavaClass.getNames()

val v<caret_type2>2: MutableList<String> = emptyList()
