// FILE: First.java

public class First<T extends Sample> {
    public static <D extends Sample> void bind(First<D> first) {}
}

// FILE: SubFirst.java

public class SubFirst<D extends Sample> extends First<D> {}

// FILE: test.kt

interface Sample

fun test(s: SubFirst<*>) {
    First.bind(s)
}