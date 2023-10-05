// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: usage.kt
<expr>
fun test() = ClassWithExternalAnnotatedMembers().notNullMethod()
</expr>

// FILE: ClassWithExternalAnnotatedMembers.java
import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    @NotNull
    public String notNullMethod() {
        return "";
    }
}
