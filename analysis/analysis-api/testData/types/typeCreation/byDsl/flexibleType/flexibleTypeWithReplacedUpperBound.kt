// FILE: SomeClass.java

public class SomeClass {
    Integer something() { return 1; }
}

// FILE: main.kt

fun foo(yy: Any) {
    val xx = SomeClass().something()
    x<caret_type>x.toString()
    y<caret_upper>y.toString()
}