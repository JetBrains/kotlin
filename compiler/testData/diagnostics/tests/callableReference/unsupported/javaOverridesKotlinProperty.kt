// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// FIR_IDENTICAL

// FILE: Foo.java
public class Foo extends Base {
    @Override
    public int getFoo() {
        return super.getFoo();
    }
}

// FILE: Main.kt
open class Base {
    open val foo: Int = 904
}

val prop = Foo::foo
