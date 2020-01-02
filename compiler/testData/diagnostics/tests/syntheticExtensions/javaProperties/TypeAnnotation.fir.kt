// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.getSomething().<!INAPPLICABLE_CANDIDATE!>length<!>
    javaClass.something.<!INAPPLICABLE_CANDIDATE!>length<!>
}

// FILE: JavaClass.java
import org.jetbrains.annotations.*;

public class JavaClass {
    @Nullable
    public String getSomething() { return null; }
}