// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: main.kt
fun usa<caret>ge(j: JavaClass.Nested) {

}

// FILE: JavaClass.java
public class JavaClass extends KotlinClass<RegularInterface, RegularInterface> {
    @Deprecated
    public static class Nested {

    }
}

// FILE: KotlinClass.kt
abstract class KotlinClass<K : RegularInterface, V: RegularInterface?> : Map<K, V>

interface RegularInterface