// `KotlinClass` does not declare `foo` itself, so its light class has no corresponding method.
// No EXPECTED directives: `asPsiMethods` returns nothing for such an intersection override.
// function: test/KotlinClass.foo

// FILE: JavaClass.java
package test;

public class JavaClass {
    public void foo() {}
}

// FILE: JavaInterface.java
package test;

public interface JavaInterface {
    void foo();
}

// FILE: main.kt
package test

class KotlinClass : JavaClass(), JavaInterface
