// FILE: constructor.java
package test;

class My {

    @constructor My() {}

    @constructor int foo() {
        return 1;
    }
}

// FILE: constructor.kt
package test

@Target(AnnotationTarget.CONSTRUCTOR)
annotation class constructor
