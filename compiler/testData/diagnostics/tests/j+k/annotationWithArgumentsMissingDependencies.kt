// FILE: A.java
@missing.Ann(x = "")
public class A {
    @missing.Ann(1)
    public String foo() {}
}

// FILE: main.kt

fun main() {
    A().foo().length
}
