// FILE: A.java

public class A {
    public int size = 1;
}

// FILE: B.java

public class B implements A {
    public int size = 1;
}

// FILE: main.kt

fun foo() {
    B().size
}
