// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface JavaInterface<T> {
    public void doSmth(@NotNull T x);
}

// FILE: KotlinInterface.kt
interface KotlinInterface<T1> : JavaInterface<T1> {
    override fun doSmth(x: <expr>T1 & Any</expr>)
}
