// FILE: setter.java
package test;

@setter class My {

    @setter int foo() {
        return 1;
    }
}

// FILE: setter.kt
package test

@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class setter
