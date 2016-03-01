// FILE: Foo.java

public class Foo {
    public class Inner { }
}

// FILE: 1.kt

fun box(): String {
    Foo().Inner()
    return "OK"
}
