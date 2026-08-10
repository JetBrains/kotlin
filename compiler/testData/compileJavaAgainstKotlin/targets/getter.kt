// FILE: getter.java
package test;

@getter class My {

    @getter int foo() {
        return 1;
    }
}

// FILE: getter.kt
package test

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class getter
