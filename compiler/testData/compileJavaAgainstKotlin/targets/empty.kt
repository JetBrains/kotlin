// FILE: empty.java
package test;

@empty class My {

    @empty int foo(@empty int i) {
        return i + 1;
    }
}

// FILE: empty.kt
package test

@Target()
annotation class empty
