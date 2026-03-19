// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53407

// FILE: JavaClass.java

import java.util.Map;

public class JavaClass {
    public JavaClass(Map copyFrom) {}

    public static void useMap(Map m) {}
}

// FILE: main.kt

// KT-53407: HashMap<…> not recognized as subtype of Java's raw Map
fun test() {
    val map = HashMap<String, String>()
    JavaClass(map)  // false positive: should be OK, HashMap<String, String> is a subtype of raw Map

    val m2: Map<String, String> = map
    JavaClass(m2)  // OK

    JavaClass.useMap(map)  // false positive: should be OK
    JavaClass.useMap(m2)  // OK
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, localProperty, propertyDeclaration */
