// MAIN_FILE_NAME: KotlinInterface
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;

public interface JavaInterface<T> {
  @NotNull
  public T doSmth(@NotNull T x);
}

// FILE: KotlinInterface.kt
interface KotlinInterface<T1> : JavaInterface<T1> {
  override fun doSmth(x: T1 & Any): T1 & Any
}
