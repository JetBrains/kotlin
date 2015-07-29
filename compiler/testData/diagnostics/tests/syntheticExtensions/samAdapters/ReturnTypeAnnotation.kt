// FILE: KotlinFile.kt
fun foo(javaInterface: JavaInterface) {
    val value: String?
    value = javaInterface.compute { "" }
    value<!UNSAFE_CALL!>.<!>length()
}

// FILE: JavaInterface.java
import org.jetbrains.annotations.*;

public interface JavaInterface {
    @Nullable
    <T> String compute(@NotNull Provider<T> provider);
}

// FILE: Provider.java
public interface Provider<T> {
    public T compute();
}
