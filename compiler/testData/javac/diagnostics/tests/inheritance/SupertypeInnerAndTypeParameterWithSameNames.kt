// FILE: a/Parent.java
package a;

public interface Parent {
    public class Foo {}
}

// FILE: a/Outer.java
package a;

public class Outer<Foo> {
    public class Inner implements Parent {
        public Foo bar() {
            return null;
        }
    }
}

// FILE: test.kt
package a

fun test() = Outer<String>().Inner().bar()