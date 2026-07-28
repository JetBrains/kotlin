// FILE: property.java
package test;

class My {
    @property int prop;

    @property int get() { return prop; }
}

// FILE: property.kt
package test

@Target(AnnotationTarget.PROPERTY)
annotation class property
