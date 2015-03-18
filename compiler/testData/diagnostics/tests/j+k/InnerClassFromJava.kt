// FILE: Bar.java
public class Bar implements Foo {
public interface I extends Boo {
}
}

// FILE: Baz.kt
public trait Foo {
    companion object {
        public val EMPTY: Foo = object : Foo{}
    }
}

trait Boo

public class Baz : Bar.I