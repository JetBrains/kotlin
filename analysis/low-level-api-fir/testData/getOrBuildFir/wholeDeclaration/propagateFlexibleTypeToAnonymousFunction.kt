// FILE: usage.kt
<expr>
val propertyToResolve: String
    get() = JavaClass.function()?.let { " ($it)" } ?: ""
</expr>

// FILE: Anno.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Anno {
    String value();
}

// FILE: JavaClass.java
public class JavaClass {
    public static @Anno("outer") List<@Anno("middle") List<@Anno("inner") Integer>> function() {
        return null;
    }
}
