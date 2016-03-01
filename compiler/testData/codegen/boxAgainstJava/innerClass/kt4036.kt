// FILE: Foo.java

public class Foo {
    public class Inner1$class {
    }

    public class Inner2$class {
    }
}

// FILE: 1.kt

fun box(): String {
    Foo().`Inner1$class`()
    Foo().`Inner2$class`()
    return "OK"
}
