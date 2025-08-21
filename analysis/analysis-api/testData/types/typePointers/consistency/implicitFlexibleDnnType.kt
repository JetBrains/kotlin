// FULL_JDK
// FILE: JavaClass.java
public final class JavaClass {
  public static <T, R> java.util.function.Function<T, R> wrap(java.util.function.Function<T, R> function) {
    return null;
  }
}

// FILE: main.kt
abstract class KotlinClass<T> {
  abstract val block: () -> T

  val propertyWithFlexibleDnnImplicitType = JavaClass.wrap(JavaClass.wrap<String, T> { _ -> block.invoke() })
}

fun <T> usage(k: KotlinClass<T>) {
  <expr>k.propertyWithFlexibleDnnImplicitType</expr>
}