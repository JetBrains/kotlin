// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// FIR_IDENTICAL

// FILE: Foo.java
public class Foo extends Base {
}

// FILE: Main.kt
open class Base {
    open val foo: Int = 904
}

val prop = Foo::foo
