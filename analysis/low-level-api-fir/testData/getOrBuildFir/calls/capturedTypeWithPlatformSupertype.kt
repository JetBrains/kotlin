// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: First.java
public class First<T extends Sample> {
    public static <D extends Sample> void bind(First<D> first) {}
}

// FILE: SubFirst.java
public class SubFirst<D extends Sample> extends First<D> {}

// FILE: main.kt
interface Sample

fun test(s: SubFirst<*>) {
    <expr>First.bind(s)</expr>
}
