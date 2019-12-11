//FILE: Foo.java
public class Foo<T extends CharSequence> {
}

//FILE: Bar.java
public interface Bar {
    void f(Foo f);
}

//FILE: a.kt
class BarImpl: Bar {
    override fun f(f: Foo<*>?) {
        throw UnsupportedOperationException()
    }
}
