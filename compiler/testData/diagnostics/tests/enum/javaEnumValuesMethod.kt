// FILE: vvv/A.java
package vvv;

public enum A {
    ENTRY,
    ANOTHER;
}

// FILE: test.kt
package vvv

fun main() {
     A.values(): Array<A>
}
