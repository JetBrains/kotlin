// FILE: base.java
package test;

@base class My {

    @base int foo(@base int i) {
        return i + 1;
    }
}

// FILE: base.kt
package test

annotation class base
