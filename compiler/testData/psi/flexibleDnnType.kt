// FILE: FlexibleDnnType.kt
abstract class FlexibleDnnType<T> {
    abstract val block: () -> T

    val propertyWithFlexibleDnnImplicitType = JavaClass.wrap(JavaClass.wrap<String, T> { _ -> block.invoke() })
}

// FILE: JavaClass.java
public final class JavaClass {
    public static <T, R> java.util.function.Function<T, R> wrap(java.util.function.Function<T, R> function) {
        return null;
    }
}
