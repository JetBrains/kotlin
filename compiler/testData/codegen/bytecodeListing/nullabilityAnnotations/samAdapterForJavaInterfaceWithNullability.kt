// FILE: samAdapterForJavaInterfaceWithNullability.kt
fun testNullable(s: String) = JNullable { s }
fun testNotNull(s: String) = JNotNull { s }
fun testNoAnnotation(s: String) = JNoAnnotation { s }

// FILE: JNullable.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JNullable {
    @Nullable String getNullableString();
}

// FILE: JNotNull.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JNotNull {
    @NotNull String getNullableString();
}

// FILE: JNoAnnotation.java
public interface JNoAnnotation {
    String getString();
}