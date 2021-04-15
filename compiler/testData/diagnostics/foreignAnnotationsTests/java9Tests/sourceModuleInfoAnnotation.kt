// ALLOW_KOTLIN_PACKAGE

// FILE: module-info.java
import org.jspecify.nullness.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
}

// FILE: Test.java
public class Test {
    void foo(Integer x) {}
}

// FILE: main.kt
fun main(x: Test) {
    x.foo(1)
}