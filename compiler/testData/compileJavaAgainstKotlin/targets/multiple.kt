// FILE: multiple.java
package test;

@multiple class My {

    @multiple int foo(@multiple int i) {
        return i + 1;
    }
}

// FILE: multiple.kt
package test

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class multiple
