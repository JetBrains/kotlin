// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;

public interface JavaInterface<T> {
    @NotNull
    public T doSmth(@NotNull T x) { return null; }
}

// FILE: KotlinInterface.kt
interface KotlinInterface<T1> : JavaInterface<T1> {
    override fun do<caret>Smth(x: T1 & Any): T1 & Any
}
