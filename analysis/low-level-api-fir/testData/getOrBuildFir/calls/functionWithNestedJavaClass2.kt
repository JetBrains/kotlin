// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-74097
// FILE: main.kt
fun function() {
    <expr>JavaClass.Nested()</expr>
}

// FILE: JavaClass.java
public class JavaClass<T extends JavaClass.Nested> extends JavaClass.Nested<T> {
    public static class Nested<T extends JavaClass.Nested> {
    }
}
