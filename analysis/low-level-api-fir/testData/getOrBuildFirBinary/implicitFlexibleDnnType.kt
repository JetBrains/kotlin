// MAIN_FILE_NAME: KotlinClass
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// FILE: JavaClass.java
public final class JavaClass {
  public static <T, R> java.util.function.Function<T, R> wrap(java.util.function.Function<T, R> function) {
    return null;
  }
}

// FILE: KotlinClass.kt
class KotlinClass<T>(private val block: () -> T) {
  val propertyWithFlexibleDnnImplicitType = JavaClass.wrap(JavaClass.wrap<String, T> { _ -> block.invoke() })
}
