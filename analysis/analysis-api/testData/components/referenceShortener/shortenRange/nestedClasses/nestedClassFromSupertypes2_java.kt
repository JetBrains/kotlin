// FILE: main.kt
package test

import dependency.JavaInterface
import dependency.JavaInterface.Nested

class Foo : JavaInterface {
    <expr>val prop: JavaInterface.Nested = JavaInterface.Nested()</expr>
}

// FILE: dependency/JavaInterface.java
package dependency;

public interface JavaInterface {
    public class Nested {}
}