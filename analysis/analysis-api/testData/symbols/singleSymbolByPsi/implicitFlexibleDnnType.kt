// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// FULL_JDK
// FILE: JavaClass.java
public final class JavaClass {
  public static <T, R> java.util.function.Function<T, R> wrap(java.util.function.Function<T, R> function) {
    return null;
  }
}

// FILE: KotlinClass.kt
class KotlinClass<T>(private val block: () -> T) {
  val property<caret>WithFlexibleDnnImplicitType = JavaClass.wrap(JavaClass.wrap<String, T> { _ -> block.invoke() })
}
