// FILE: function.java
package test;

@function class My {

    @function int foo() {
        return 1;
    }
}

// FILE: function.kt
package test

@Target(AnnotationTarget.FUNCTION)
annotation class function
