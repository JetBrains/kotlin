// FIR_IDENTICAL
// SKIP_TXT
// FILE: JavaClass.java
public class JavaClass extends ContainerType<Container<JavaClass.Nested>> {
    public static class Nested extends Container<String> {}
}

// FILE: ContainerType.java
public class ContainerType<T> {}

// FILE: Container.java
public class Container<K> {}

// FILE: Usage.java
public class Usage {
    public static JavaClass.Nested foo() { return null; }
}

// FILE: main.kt

fun main() {
    Usage.foo().hashCode()
}
