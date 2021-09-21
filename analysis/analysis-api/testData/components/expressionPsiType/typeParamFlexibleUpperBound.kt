// FILE: kotlin.kt

fun bar() {
    val xx = JavaClass::<expr>foo</expr>
}

// FILE: JavaClass.java

import org.jetbrains.annotations.NotNull;

public class JavaClass {
    @NotNull
    public static <T extends B> T foo(@NotNull T x) {
        return x;
    }
}

// FILE: B.java
public class B {

}