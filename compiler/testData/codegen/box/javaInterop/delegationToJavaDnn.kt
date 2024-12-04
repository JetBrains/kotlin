// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// ISSUE: KT-55357

// MODULE: lib
// FILE: J.java
import org.jetbrains.annotations.NotNull;

public interface J {
    @NotNull
    <T> T foo();
}

// FILE: JImpl.java
public class JImpl implements J {
    public <T> T foo() {
        return (T) "OK";
    }
}
// FILE: a.kt
class C(val x: J) : J by x

// MODULE: main(lib)
// FILE: a.kt

fun box(): String {
    return C(JImpl()).foo<String>()
}
