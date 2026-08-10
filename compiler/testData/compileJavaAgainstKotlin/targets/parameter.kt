// FILE: parameter.java
package test;

@parameter class My {

    @parameter int foo(@parameter int i) {
        return i + 1;
    }
}

// FILE: parameter.kt
package test

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class parameter
