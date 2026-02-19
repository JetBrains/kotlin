// FILE: J.java
public abstract class J<T> {
    public abstract Foo<T> getFoo();
}

// FILE: Foo.java
public interface Foo<T> {
}

// FILE: main.kt
fun <T> main(c: J<T>) {
    if (c is K) {
        c.fo<caret>o
    }
}

class K : J<Any?>() {
    override fun getFoo(): Foo<Any?> {
        TODO("Not yet implemented")
    }
}
