// FIR_IDENTICAL

// FILE: Jaba.java
public class Jaba {
    public void foo(Object obj) {}
}

// FILE: main.kt
class Foo : Jaba() {
    override fun foo(obj: Any) {}
}
