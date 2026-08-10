// FILE: kt3698.java
package test;

public class kt3698 {

    @interface Foo {
        int value();
    }

    @Foo(KotlinClass.FOO) // Error here
    public static void main(String[] args) {
        System.out.println(KotlinClass.FOO);
    }
}

// FILE: kt3698.kt
package test

class KotlinClass {
    companion object {
        const val FOO: Int = 10
    }
}
