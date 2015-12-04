// !CHECK_TYPE

// FILE: A.java

public class A {
    public int size = 1;
}

// FILE: B.java

public class B extends A {
    public String size = 1;
}

// FILE: main.kt

fun foo() {
    B().size.checkType { _<String>() }
}
