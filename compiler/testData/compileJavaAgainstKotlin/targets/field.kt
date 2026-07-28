// FILE: field.java
package test;

class My {
    @field int prop;

    @field int get() { return prop; }
}

// FILE: field.kt
package test

@Target(AnnotationTarget.FIELD)
annotation class field
