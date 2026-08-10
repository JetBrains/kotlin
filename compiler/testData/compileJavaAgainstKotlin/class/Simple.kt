// FILE: Simple.java
package test;

class Simple {
    {
        new Impossible<String>();
    }
}

// FILE: Simple.kt
package test

class Impossible<P>()
