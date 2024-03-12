// WITH_STDLIB
// FILE: AnnRaw.java
public @interface AnnRaw {
    Class value();
}

// FILE: Ann.java
public @interface Ann {
    Class<?> value();
}

// FILE: Utils.java
public class Utils {
    public static void foo(Class value) {}
    public static void fooRaw(Class<?> value) {}
}

// FILE: main.kt

class X

@Ann(X::class)
@AnnRaw(X::class)
fun test() {
    Utils.foo(X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>)
    Utils.fooRaw(X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>)
}
