// FILE: classifier.java
package test;

@classifier class My {

    @classifier int foo() {
        return 1;
    }
}

// FILE: classifier.kt
package test

@Target(AnnotationTarget.CLASS)
annotation class classifier
