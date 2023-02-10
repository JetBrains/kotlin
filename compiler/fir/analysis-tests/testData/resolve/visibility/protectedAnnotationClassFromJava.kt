// ISSUE: KT-47870

// FILE: example/JavaSuper.java
package example;

public abstract class JavaSuper {
    protected @interface Foo {
        String value() default "";
    }
}

// FILE: samePackage.kt
package example

class KotlinChildOfJavaSuper : JavaSuper() {
    @Foo("should work")
    fun usesFoo() = ""
}

// FILE: otherPackage.kt
import example.JavaSuper

class KotlinChildOfJavaSuper : JavaSuper() {
    @Foo("should work")
    fun usesFoo() = ""
}
