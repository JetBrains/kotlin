// FILE: KotlinFile.kt
fun foo(javaInterface: JavaInterface) {
    val value = javaInterface.compute { "" }
    value.<!INAPPLICABLE_CANDIDATE!>length<!>
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
