// FILE: J.java
public class J extends K<String> {
    @Override
    public Foo<String> getFoo() { return null; }
}

// FILE: test.kt
abstract class K<T> {
    abstract val foo: Foo<T>
}

class Foo<T>

fun <T> test(c: K<T>) {
    if (c is J) {
        c.fo<caret>o
    }
}
