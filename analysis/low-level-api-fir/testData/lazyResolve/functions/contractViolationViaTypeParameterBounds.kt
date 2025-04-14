// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: main.kt
fun usa<caret>ge(j: JavaClass.Nested) {

}

// FILE: JavaClass.java
public class JavaClass extends KotlinClass<KotlinInterface> {
    @Deprecated
    public static class Nested {

    }
}

// FILE: KotlinClass.kt
abstract class KotlinClass<T : RegularInterface> : InterfaceWithGeneric<T>

interface RegularInterface
interface InterfaceWithGeneric<T>