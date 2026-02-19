// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface JavaInterface<T> {
    public void doSmth(@NotNull List<@NotNull T> x) { return null; }
}

// FILE: KotlinInterface.kt
interface KotlinInterface<T1> : JavaInterface<T1> {
    override fun doSmth(x: <expr>List<T1 & Any></expr>)
}
