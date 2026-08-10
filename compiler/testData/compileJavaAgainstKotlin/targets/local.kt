// FILE: local.java
package test;

class My {

    int foo(@local int i) {
        @local int j = i + 1;
        return j;
    }
}

// FILE: local.kt
package test

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class local
